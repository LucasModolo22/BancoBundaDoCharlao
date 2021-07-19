package br.com.bbc.banco.enumeration;

public enum BotEnumeration {

    PREFIX("$"),
    TOKEN(System.getenv("TOKEN")),
    CURRENCY("BC$"),
    RED(0x7f2927),
    GREEN(0x80b461),
    YELLOW(0xd0a843),
    BLACK(0x1FFFFFFF),
    INVITE_LINK("https://discord.com/api/oauth2/authorize?client_id=826577440549502976&permissions=8&scope=applications.commands%20bot");

    private String value;
    private int number;

    BotEnumeration(String value){
        this.value = value;
    }
    BotEnumeration(int number){
        this.number = number;
    }

    public String getText(){
        return value;
    }

    public int getNumber(){
        return number;
    }

}
