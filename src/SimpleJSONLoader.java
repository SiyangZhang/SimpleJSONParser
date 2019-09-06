import java.util.HashMap;
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
        this.input = input;
        buffer = "";
        dataMap = new HashMap<>();
    }
    public char peek(){
        return input.charAt(cursor);
    }

    public char match(char c) throws Exception{
        if(c == peek()){
            cursor ++;
            System.out.println("match: " + c);
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

    public String parse() throws Exception{
        return parseJSON("");
    }


    public String parseJSON(String buffer) throws Exception{

        String left = "" + match('{');
        String oldbuffer = buffer;
        buffer += "\t";
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
        res += parseKey();
        res += match(':');
        res += parseValue(buffer);
        return res;
    }

    public String parseTupleListTail(String buffer) throws Exception{
        String res = "";
        if(peek() == ','){
            System.out.println("buffer length: " + buffer.length());
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
                while(peek() != '"'){
                    res += freeMatch();
                }
                res += match('"');
                break;
            case '{':
                res += parseJSON(buffer);
                break;
            case '[':
                res += parseList(buffer);
                break;
            default:
                if(isDigit(peek())){
                    res += parseNumber();
                }else{
                    throw new Exception("Not a correctly formatted value.");
                }

        }

        return res;
    }

    public String parseList(String buffer) throws Exception{
        String res = "";
        res += "\n" + buffer + match('[');
        String oldbuffer = buffer;
        buffer += "\t";
        res += "\n" + buffer + parseValue(buffer);
        res += parseListTail(buffer);
        res += "\n" + oldbuffer + match(']');
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

    public String parseNumber() throws Exception{
        String num = "";
        while(isDigit(peek())){
            num += freeMatch();
        }
        return num;
    }


    public static void main(String[] args){
        String input = "{\"abc\":\"123\",\"list\":[1,{\"name\":\"YJSNP\",\"nationality\":\"Japanese\",\"party info\":" +
                "{\"party name\":\"black tea\",\"lucky numbers\":[114514,1919810]}},3,4,5],\"age\":18}";
        SimpleJSONLoader parser = new SimpleJSONLoader(input);

        try{
            String res = parser.parse();
            System.out.println("\n-----------------------------------------------------");
            System.out.println(res);
        }catch(Exception e){
            System.out.println(parser.input.substring(0,parser.cursor));
            System.out.println(parser.cursor);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


}
