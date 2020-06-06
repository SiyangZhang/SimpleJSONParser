import java.util.HashMap;
import java.util.Map;

/**
 * @Author Siyang Zhang
 * @Date Sept 5, 2019
 * */

public class SimpleJSONParser {

    int cursor;

    String input;

    String buffer;

    boolean consumeWhiteSpace;
    
    boolean printLog;

    Map<String, Object> dataMap;


    public SimpleJSONParser(){
        cursor = 0;
        input = "";
        buffer = "";
        consumeWhiteSpace = true;
        dataMap = new HashMap<>();
    }

    public SimpleJSONParser(String input){
        cursor = 0;
        this.input = input;
        buffer = "";
        consumeWhiteSpace = true;
        dataMap = new HashMap<>();
    }
    public char peek(){
        return input.charAt(cursor);
    }

    public char match(char c) throws Exception{
        if(consumeWhiteSpace){
            consumeWhiteSpace();
        }
        if(c == peek()){
            cursor ++;
            printMessage("match: '" + c + "', at pos " + cursor);
            if(consumeWhiteSpace){
                consumeWhiteSpace();
            }
            return c;
        }else{
            throw new Exception("Mismatch: expect '" + c + "', but see '" + peek() + "'. Previous char is: '" + input.charAt(cursor-1) + "'");
        }
    }

    public char freeMatch() throws Exception{
        printMessageWithoutNewLine("free ");
        return match(peek());
    }

    public void consumeWhiteSpace(){
        if(cursor >= input.length()){
            return;
        }
        printMessage("consume white space start at " + cursor);
        while(peek() == ' ' || peek() == '\n' || peek() == '\t'){
            if(cursor < input.length() - 1){
                cursor++;
            }else{
                return;
            }
        }
        printMessage("consume white space end at " + cursor);
    }

    public boolean isLetter(char c){
        int ord = c;
        return (ord >= 65 && ord <= 90) || (ord >= 97 && ord <= 122);
    }

    public boolean isDigit(char c){
        int val = c - '0';
        return val >= 0 && val <= 9;
    }

    public boolean isLegal(char c){
        return isLetter(c) || isDigit(c) || c == '_';
    }

    public String parse() throws Exception{
        return parseJSON("");
    }


    public String parseJSON(String buffer) throws Exception{

        String left = "" + match('{');
        String oldbuffer = buffer;
        buffer += "\t";
        consumeWhiteSpace();
        if(peek() == '"'){
            String tuplelist = parseTupleList(buffer);
            String right = "" + match('}');
            return left + "\n" + buffer + tuplelist + "\n" + oldbuffer + right;
        }else{
            left += match('}');
            return left;
        }
    }

    public String parseTupleList(String buffer) throws Exception{
        String first = parseTuple(buffer);
        String rest = parseTupleListTail(buffer);
        return first + rest;

    }

    public String parseTuple(String buffer) throws Exception{
        String res = "";
        String key = parseKey();
        res += key;
        printMessage("In json tuple, parse key '" + key + "'");
        res += match(':');
        res += parseValue(buffer);
        return res;
    }

    public String parseTupleListTail(String buffer) throws Exception{
        String res = "";
        if(peek() == ','){
            printMessage("buffer length: " + buffer.length());
            res += match(',')+"\n"+buffer;
            res += parseTuple(buffer);
            res += parseTupleListTail(buffer);
        }else{
            return "";
        }

        return res;
    }

    public String parseKey() throws Exception{
        String key = "";
        key += match('"');
        key += parseNonEmptyToken();
        key += match('"');
        return key;
    }

    public String parseValue(String buffer) throws Exception{
        String res = "";
        char start = peek();
        switch (start){
            case '"':
                res += match('"');
                consumeWhiteSpace = false;
                while(peek() != '"'){
                    res += freeMatch();
                }
                res += match('"');
                consumeWhiteSpace = true;
                break;
            case '{':
                res += parseJSON(buffer);
                break;
            case '[':
                res += parseList(buffer);
                break;
            default:
                printMessage("here 1");
                if(isDigit(peek()) || peek() == '-'){
                    printMessage("parse value to number");
                    res += parseNumber();
                }else{
                    res += parseReservedWords();
                }

        }

        return res;
    }

    public String parseList(String buffer) throws Exception{
        String res = "";
        res += "\n" + buffer + match('[');
        if(peek() == ']'){
            res += match(']');
        }else {
            String oldbuffer = buffer;
            buffer += "\t";
            res += "\n" + buffer + parseValue(buffer);
            res += parseListTail(buffer);
            res += "\n" + oldbuffer + match(']');
        }
        return res;
    }



    public String parseListTail(String buffer) throws Exception{

        char start = peek();
        String res = "";
        if(start == ','){
            res += match(',')+"\n";
            res += buffer + parseValue(buffer);
            res += parseListTail(buffer);
        }else{
            return "";
        }
        return res;
    }

    public String parseToken() throws Exception{
        String token = "";
        consumeWhiteSpace = false;
        while(isLegal(peek()) || peek() == ' '){
            token += freeMatch();
        }
        consumeWhiteSpace = true;
        return token;
    }

    public String parseNonEmptyToken() throws Exception{
        String res = parseToken();
        if(res.length() == 0){
            throw new Exception("Find an empty token");
        }else{
            return res;
        }
    }

    public String parseNumber() throws Exception{
        String num = "";
        int tempCur = cursor;
        while(isDigit(peek()) || peek() == '.' || peek() == '-'){
            cursor++;
        }
        num = input.substring(tempCur, cursor);

        try{
            Double.parseDouble(num);
        }catch (Exception e){
            cursor = tempCur;
            throw new Exception("Fail to parse a number at position " + cursor + ".");
        }
        return num;
    }

    public String parseReservedWords() throws Exception{
        String word = "";
        if(peek() == 'n'){
            try{
                word += match('n');
                word += match('u');
                word += match('l');
                word += match('l');
            }catch(Exception e){
                throw new Exception("When parsing word 'null', " + e.getMessage());
            }
        }else if(peek() == 't'){
            try{
                word += match('t');
                word += match('r');
                word += match('u');
                word += match('e');
            }catch(Exception e){
                throw new Exception("When parsing word 'true', " + e.getMessage());
            }
        }else if(peek() == 'f'){
            try{
                word += match('f');
                word += match('a');
                word += match('l');
                word += match('s');
                word += match('e');
            }catch(Exception e){
                throw new Exception("When parsing word 'false', " + e.getMessage());
            }
        }else{
            throw new Exception("Not a proper reserved word.");
        }
        return word;
    }
    
    public void printMessage(String s){
        if(printLog) {
            System.out.println(s);
        }
    }

    public void printMessageWithoutNewLine(String s){
        if(printLog){
            System.out.println(s);
        }
    }


    public static void main(String[] args){
        String input = "{\"abc\":\"123\",\"list\":[1,{\"name\":\"YJSNP\",\"nationality\":\"Japanese\",\"party info\":" +
                "{\"party name\":\"black tea\",\"lucky numbers\":[114514,1919810]}},3,4,5],\"age\":18}";
        String jsonStr = "{\n" +
                "    \"finish_order\": \"true\",\n" +
                "    \"result\": {\n" +
                "        \"city\": \"深圳市\",\n" +
                "        \"departure_date\": \"2020-06-07\",\n" +
                "        \"district\": \"罗湖区\",\n" +
                "        \"product_infos\": {\n" +
                "            \"mostGeneralScoreProductBo\": {\n" +
                "                \"roomNums\": 0,\n" +
                "                \"amountToPay\": 0,\n" +
                "                \"themeCategory\": [\n" +
                "                    \"特价频道\"\n" +
                "                ],\n" +
                "                \"rate_type_name\": null,\n" +
                "                \"endDate\": \"2020-06-07\",\n" +
                "                \"smokeInfo\": \"\",\n" +
                "                \"bed_types\": null,\n" +
                "                \"source\": null,\n" +
                "                \"booking_notice\": null,\n" +
                "                \"cancel_policy\": null,\n" +
                "                \"children\": null,\n" +
                "                \"can_book\": true,\n" +
                "                \"price\": 189,\n" +
                "                \"star_score\": null,\n" +
                "                \"bathroom\": null,\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"room_type_name\": \"标准大床房\",\n" +
                "                \"wifi\": null,\n" +
                "                \"pateoIsGuarantee\": \"false\",\n" +
                "                \"bed_type\": null,\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"ratePlanCategory\": \"501\",\n" +
                "                \"roomtypeid\": 18178020,\n" +
                "                \"room_area\": \"10-15\",\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"plans\": null,\n" +
                "                \"breakfast\": 0,\n" +
                "                \"sourceAPI\": \"pateo\",\n" +
                "                \"supplier_code\": null,\n" +
                "                \"startDate\": \"2020-06-06\",\n" +
                "                \"price_score\": null,\n" +
                "                \"adults\": null,\n" +
                "                \"image_urls\": null,\n" +
                "                \"avg_price\": 189,\n" +
                "                \"capacity\": null,\n" +
                "                \"extra\": null,\n" +
                "                \"hotel\": {\n" +
                "                    \"address\": \"华强北振兴西路304栋3楼西侧\",\n" +
                "                    \"star\": 2,\n" +
                "                    \"distance\": null,\n" +
                "                    \"city\": \"深圳市\",\n" +
                "                    \"image_url\": \"http://dimg04.c-ctrip.com/images//200511000000rjugn8469_R_550_412.jpg\",\n" +
                "                    \"latitude\": \"22.54713\",\n" +
                "                    \"brand_name\": \"\",\n" +
                "                    \"score\": 4.6,\n" +
                "                    \"facilitySet\": null,\n" +
                "                    \"distanceFromSpecial\": 324,\n" +
                "                    \"phone\": \"0755-83219802\",\n" +
                "                    \"price\": \"179.0\",\n" +
                "                    \"intro\": null,\n" +
                "                    \"district\": \"福田区\",\n" +
                "                    \"sourceHotel\": {\n" +
                "                        \"commServiceRate\": 0,\n" +
                "                        \"address\": \"华强北振兴西路304栋3楼西侧\",\n" +
                "                        \"ctripStarRate\": 0,\n" +
                "                        \"hotelStarRate\": 2,\n" +
                "                        \"distance\": 324,\n" +
                "                        \"latitude\": 22.54713,\n" +
                "                        \"commFacilityRate\": 0,\n" +
                "                        \"hotelCode\": 5074676,\n" +
                "                        \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                        \"commSurroundingRate\": 0,\n" +
                "                        \"ctripCommRate\": 4.6,\n" +
                "                        \"commCleanRate\": 0,\n" +
                "                        \"phone\": \"0755-83219802\",\n" +
                "                        \"minPrice\": 179,\n" +
                "                        \"ctripUserRate\": 0,\n" +
                "                        \"position\": \"114.082796,22.54713\",\n" +
                "                        \"hotelIcon\": \"http://dimg04.c-ctrip.com/images//200511000000rjugn8469_R_550_412.jpg\",\n" +
                "                        \"longitude\": 114.082796\n" +
                "                    },\n" +
                "                    \"name\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                    \"generalScore\": 61.714285714285715,\n" +
                "                    \"id\": 5074676,\n" +
                "                    \"facilities\": null,\n" +
                "                    \"sourceAPI\": \"pateo\",\n" +
                "                    \"longitude\": \"114.082796\"\n" +
                "                },\n" +
                "                \"guaranteeCode\": \"4\",\n" +
                "                \"arrival_start_time\": null,\n" +
                "                \"place\": 1,\n" +
                "                \"floor\": null,\n" +
                "                \"dist_score\": null,\n" +
                "                \"general_score\": null,\n" +
                "                \"sourceBody\": null,\n" +
                "                \"services\": [\n" +
                "                    \"电视机\",\n" +
                "                    \"无烟楼层\",\n" +
                "                    \"无充电车位\",\n" +
                "                    \"洗衣服务\",\n" +
                "                    \"有可无线上网的公共区域 免费\",\n" +
                "                    \"24小时热水\",\n" +
                "                    \"空调\",\n" +
                "                    \"有可无线上网的公共区域\",\n" +
                "                    \"吹风机\",\n" +
                "                    \"手动窗帘\",\n" +
                "                    \"叫醒服务\",\n" +
                "                    \"电热水器\",\n" +
                "                    \"行李寄存\",\n" +
                "                    \"24小时前台服务\",\n" +
                "                    \"电梯\",\n" +
                "                    \"床具:毯子或被子\",\n" +
                "                    \"所有公共及私人场所严禁吸烟\",\n" +
                "                    \"收费停车场\"\n" +
                "                ],\n" +
                "                \"meal\": \"0\",\n" +
                "                \"sourceBodyDetail\": {\n" +
                "                    \"channellimitInfo\": null,\n" +
                "                    \"reservetimelimitinfo\": {\n" +
                "                        \"lastreservetime\": \"2020-06-07T07:00:00.000+08:00\"\n" +
                "                    },\n" +
                "                    \"holddeadline\": null,\n" +
                "                    \"isguaranteed\": true,\n" +
                "                    \"addbedfee\": -100,\n" +
                "                    \"applicabilityinfo\": {\n" +
                "                        \"applicability\": \"10000000\",\n" +
                "                        \"otherdescription\": \"\"\n" +
                "                    },\n" +
                "                    \"isinterface\": false,\n" +
                "                    \"roomGiftInfo\": null,\n" +
                "                    \"isshowagent\": false,\n" +
                "                    \"roomname\": \"标准大床房\",\n" +
                "                    \"arearange\": \"10-15\",\n" +
                "                    \"smokeinfo\": {\n" +
                "                        \"hassmokecleanroom\": \"\",\n" +
                "                        \"notallowsmokingcode\": \"\",\n" +
                "                        \"hasnonsmokeroom\": \"\",\n" +
                "                        \"nononsmokeroom\": \"\",\n" +
                "                        \"hasroominnonsmokearea\": \"\",\n" +
                "                        \"notallowsmoking\": \"\"\n" +
                "                    },\n" +
                "                    \"children\": 1,\n" +
                "                    \"hasWindow\": 0,\n" +
                "                    \"issupportanticipation\": true,\n" +
                "                    \"invoicetargettype\": 2,\n" +
                "                    \"roomfgtoppinfo\": {\n" +
                "                        \"canfgtopp\": false,\n" +
                "                        \"isfgtopp\": false\n" +
                "                    },\n" +
                "                    \"roomcurrencyinfo\": {\n" +
                "                        \"exchange\": 1,\n" +
                "                        \"currencyname\": \"人民币\",\n" +
                "                        \"currency\": \"RMB\"\n" +
                "                    },\n" +
                "                    \"anticipationcoefficient\": 1,\n" +
                "                    \"roompriceinfo\": {\n" +
                "                        \"isguarantee\": false,\n" +
                "                        \"iscanreserve\": true,\n" +
                "                        \"roompricedetail\": [\n" +
                "                            {\n" +
                "                                \"effectdate\": \"2020-06-06T00:00:00.000+08:00\",\n" +
                "                                \"roomstatus\": \"G\",\n" +
                "                                \"price\": {\n" +
                "                                    \"amount\": \"189\",\n" +
                "                                    \"cnyamount\": \"189\",\n" +
                "                                    \"currency\": \"RMB\"\n" +
                "                                },\n" +
                "                                \"guaranteecode\": \"4\",\n" +
                "                                \"breakfast\": 0\n" +
                "                            }\n" +
                "                        ],\n" +
                "                        \"averageprice\": {\n" +
                "                            \"amount\": \"189\",\n" +
                "                            \"cnyamount\": \"189\",\n" +
                "                            \"currency\": \"CNY\"\n" +
                "                        },\n" +
                "                        \"isjustifyconfirm\": true,\n" +
                "                        \"rateplancategory\": \"501\",\n" +
                "                        \"remainingrooms\": \"8\",\n" +
                "                        \"paytype\": \"PP\"\n" +
                "                    },\n" +
                "                    \"roombedtypeinfo\": {\n" +
                "                        \"haskingbed\": \"T\",\n" +
                "                        \"singlebedwidth\": \"0\",\n" +
                "                        \"hassinglebed\": \"F\",\n" +
                "                        \"hastwinbed\": \"F\",\n" +
                "                        \"kingbedwidth\": \"1.8\",\n" +
                "                        \"twinbedwidth\": \"0\"\n" +
                "                    },\n" +
                "                    \"floorrange\": \"2-3\",\n" +
                "                    \"memberlimitinfo\": {\n" +
                "                        \"gold\": false,\n" +
                "                        \"general\": false,\n" +
                "                        \"platinum\": false,\n" +
                "                        \"diamond\": false,\n" +
                "                        \"wechat\": false,\n" +
                "                        \"edm\": false\n" +
                "                    },\n" +
                "                    \"broadnetinfo\": {\n" +
                "                        \"wirelessbroadnetfee\": 1,\n" +
                "                        \"haswiredbroadnet\": \"F\",\n" +
                "                        \"haswirelessbroadnet\": \"T\",\n" +
                "                        \"wiredbroadnetfee\": 0,\n" +
                "                        \"broadnetfeedetail\": \"0\",\n" +
                "                        \"hasbroadnet\": 1,\n" +
                "                        \"wirelessbroadnetroom\": 1,\n" +
                "                        \"wiredbroadnetroom\": 1\n" +
                "                    },\n" +
                "                    \"ishourroom\": false,\n" +
                "                    \"roomid\": 784980250,\n" +
                "                    \"roomticketgifts\": [],\n" +
                "                    \"cancellimitinfo\": {\n" +
                "                        \"policytype\": \"2\",\n" +
                "                        \"lastcanceltime\": \"2020-06-06T18:00:00.000+08:00\"\n" +
                "                    },\n" +
                "                    \"person\": 2,\n" +
                "                    \"promotioninfo\": {\n" +
                "                        \"text\": \"\"\n" +
                "                    },\n" +
                "                    \"roomTags\": []\n" +
                "                },\n" +
                "                \"broadnet\": null,\n" +
                "                \"mark_score\": null,\n" +
                "                \"timely\": null,\n" +
                "                \"arrival_end_time\": null,\n" +
                "                \"pateoIsCanReserve\": true,\n" +
                "                \"is_abroad_price\": null,\n" +
                "                \"window\": null,\n" +
                "                \"customer_types\": null\n" +
                "            }\n" +
                "        },\n" +
                "        \"current_recommend_index\": 0,\n" +
                "        \"sourceAPI\": \"pateo\",\n" +
                "        \"arrival_date\": \"2020-06-06\",\n" +
                "        \"all_rooms\": [\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"784980250\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.8\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200413000000v0y5o3316_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200h13000000v3jbs9757_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000v20hbE70D_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200k14000000w6l7qAD79_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178020\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"标准大床房\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 189,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"leaveTime\": \"2020-06-07\",\n" +
                "                \"rate_type_id\": \"821040321\",\n" +
                "                \"bedSize\": {\n" +
                "                    \"haskingbed\": \"T\",\n" +
                "                    \"singlebedwidth\": \"0\",\n" +
                "                    \"hassinglebed\": \"F\",\n" +
                "                    \"hastwinbed\": \"F\",\n" +
                "                    \"kingbedwidth\": \"1.5\",\n" +
                "                    \"twinbedwidth\": \"0\"\n" +
                "                },\n" +
                "                \"roomPictures\": [\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200v13000000vd0gsD709_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200c13000000v4lws0508_R_550_412.jpg\",\n" +
                "                    \"http://dimg04.c-ctrip.com/images//200t13000000vdbq6B3FA_R_550_412.jpg\"\n" +
                "                ],\n" +
                "                \"room_type_id\": \"18178021\",\n" +
                "                \"hotelId\": \"5074676\",\n" +
                "                \"hotelName\": \"城市便捷酒店(深圳华强北地铁站店)\",\n" +
                "                \"roomName\": \"特惠大床房(无窗)\",\n" +
                "                \"meal\": \"0\",\n" +
                "                \"price\": 179,\n" +
                "                \"roomSize\": \"\",\n" +
                "                \"enterTime\": \"2020-06-06\",\n" +
                "                \"canReserve\": true\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"search_result_type\": \"50001\"\n" +
                "}";
        SimpleJSONParser parser = new SimpleJSONParser(input);
        SimpleJSONParser parser2 = new SimpleJSONParser(jsonStr);
        try{
            String res = parser2.parse();
            System.out.println("\n-----------------------------------------------------");

            
        }catch(Exception e){
            System.out.println(parser2.input.substring(0,parser.cursor));
            System.out.println(parser2.cursor);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


}
