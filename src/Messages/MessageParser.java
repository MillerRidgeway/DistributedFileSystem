package Messages;

import java.util.Map;

public class MessageParser {
    private String parsedKV, key, value;

    public MessageParser(String toBeParsed){
        //Check to see if matches message format
        if(!(toBeParsed.charAt(0) == '{')){
            System.err.println(toBeParsed + " :data does not match message format, now exiting");
            System.exit(0);
        }

        String trimmedInput = toBeParsed.replace("{","");
        trimmedInput = trimmedInput.replace("}","");
        trimmedInput = trimmedInput.trim();

        String [] kv = trimmedInput.split(":");

        this.parsedKV = trimmedInput;
        this.key = kv[0];
        this.value = kv[1];
    }

    //Converts the given <K, V> pair to a string for sending
    public static String mapToString(String key, Map<String, String> index){
        return "{" + key + ":" + index.get(key) + "}";
    }

    public String getParsedKV() {
        return parsedKV;
    }

    public String getKey(){
        return key;
    }

    public String getValue(){
        return value;
    }
}
