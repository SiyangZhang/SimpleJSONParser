import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Siyang Zhang
 * @Date Sept 6, 2019
 * */

public class SimpleJSONLoader {

    int cursor;

    String input;

    String buffer;

    Map<String, Object> dataMap;


    public SimpleJSONLoader(){
        cursor = 0;
        input = "";
        buffer = "";
        dataMap = new HashMap<>();
    }

    public SimpleJSONLoader(String input){
        cursor = 0;
        this.input = input+"?";
        buffer = "";
        dataMap = new HashMap<>();
    }
    public char peek(){
        return input.charAt(cursor);
    }

    public char match(char c) throws Exception{
        consumeWhiteSpace();
        if(c == peek()){
            cursor ++;
            System.out.println("match: " + c);
            consumeWhiteSpace();
            return c;
        }else{
            throw new Exception("Mismatch: expect '" + c + "', but see '" + peek() + "'.");
        }
    }

    public char freeMatch() throws Exception{
        System.out.print("free ");
        return match(peek());
    }

    public void consumeWhiteSpace(){
        while(peek() == ' ' || peek() == '\n' || peek() == '\t'){
            if(cursor < input.length() - 1){
                cursor++;
            }else{
                return;
            }
        }
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

    public void parse() throws Exception{
        dataMap = parseJSON("");
    }


    public Map<String,Object> parseJSON(String buffer) throws Exception{
        Map<String,Object> map = new HashMap<>();

        match('{');

        if(peek() == '"'){
            List<Tuple<Object>> tuplelist = parseTupleList(buffer);
            match('}');
            for(int i = 0; i < tuplelist.size(); i++){
                Tuple<Object> tuple = tuplelist.get(i);
                map.put(tuple.getKey(), tuple.getValue());
            }
            return map;
        }else{
            match('}');
        }

        return map;
    }

    public List<Tuple<Object>> parseTupleList(String buffer) throws Exception{
        Tuple<Object> first = parseTuple(buffer);
        List<Tuple<Object>> rest = parseTupleListTail(buffer);
        if(rest.size() == 0){
            rest.add(first);
            return rest;
        }
        rest.add(0,first);
        return rest;

    }

    public Tuple<Object> parseTuple(String buffer) throws Exception{

        String key = parseKey();
        match(':');
        Object value = parseValue(buffer);
        return new Tuple<>(key, value);
    }

    public List<Tuple<Object>> parseTupleListTail(String buffer) throws Exception{
        String res = "";
        List<Tuple<Object>> list = new ArrayList<>();
        if(peek() == ','){
            match(',');
            Tuple<Object> tuple = parseTuple(buffer);
            List<Tuple<Object>> tail =  parseTupleListTail(buffer);
            if(tail.size() == 0){
                tail.add(tuple);
                return tail;
            }else{
                tail.add(0,tuple);
                return tail;
            }
        }else{
            return list;
        }

    }

    public String parseKey() throws Exception{
        String key = "";
        key += match('"');
        key += parseNonEmptyToken();
        key += match('"');
        return key;
    }

    public Object parseValue(String buffer) throws Exception{
        String res = "";
        char start = peek();
        switch (start){
            case '"':
                res += match('"');
                while(peek() != '"'){
                    res += freeMatch();
                }
                res += match('"');
                break;
            case '{':
                res += parseJSON(buffer);
                break;
            case '[':
                List<Object> list = parseList(buffer);
                res += list.toString();
                return list;
            default:
                if(isDigit(peek())){
                    res += parseNumber();
                }else{
                    throw new Exception("Not a correctly formatted value.");
                }

        }

        return res;
    }

    public List<Object> parseList(String buffer) throws Exception{

        List<Object> list = new ArrayList<>();
        match('[');

        Object value = parseValue(buffer);
        list.add(value);
        parseListTail(list);
        match(']');

        return list;
    }



    public String parseListTail(List<Object> list) throws Exception{

        char start = peek();
        String res = "";
        if(start == ','){
            match(',');
            list.add(parseValue(buffer));
            parseListTail(list);
        }else{
            return "";
        }
        return res;
    }

    public String parseToken() throws Exception{
        String token = "";
        while(isLegal(peek()) || peek() == ' '){
            token += freeMatch();
        }
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

    public Number parseNumber() throws Exception{
        String num = "";
        while(isDigit(peek()) || peek() == '.'){
            num += freeMatch();
        }
        try{
            int s = Integer.parseInt(num);
            return s;
        }catch(NumberFormatException ex){
            double d = Double.parseDouble(num);
            return d;
        }
    }


    public static void main(String[] args){
        String input = "{\"abc\" : 12.34,\"list\" : [1,{ \"name\":\"YJSNP\",\"nationality\" : \"Japanese\",\"party info\":" +
                "{ \"party name\" : \"black tea\",\"lucky numbers\":[114514,1919810]}},3,4,5],\"age\":18}";
        SimpleJSONLoader parser = new SimpleJSONLoader(input);

        try{
            parser.parse();
            System.out.println("\n-----------------------------------------------------");
            Map<String, Object> map = parser.dataMap;
            for(String key : map.keySet()){
                System.out.println(key + "   " + map.get(key));
            }
        }catch(Exception e){
            System.out.println(parser.input.substring(0,parser.cursor));
            System.out.println(parser.cursor);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


}
