public class MessageParser {
    private String parsedKV, key, value;

    public MessageParser(String toBeParsed){
        //Check to see if matches message format
        if(!(toBeParsed.charAt(0) == '{')){
            System.err.println("Data does not match message format, now exiting");
            System.exit(0);
        }

        String trimmedInput = toBeParsed.replace("{","");
        trimmedInput.replace("}","");
        trimmedInput.trim();

        String [] kv = trimmedInput.split(":");

        this.parsedKV = trimmedInput;
        this.key = kv[0];
        this.value = kv[1];
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
