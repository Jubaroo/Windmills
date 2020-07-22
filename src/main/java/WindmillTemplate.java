public class WindmillTemplate {

    public String sound;
    public String model;
    public String name;
    public int templateProduce;
    public int templateConsume;
    public int templateSecondaryConsume;
    public int maxNum;
    public int maxItems;
    public long timeout;
    public int templateID;

    public WindmillTemplate(String name, String sound, String model, int templateProduce, int templateConsume, int templateSecondaryConsume, int maxNum, int maxitems, long timeout) {
        this.sound = sound;
        this.model = model;
        this.name = name;
        this.templateProduce = templateProduce;
        this.templateConsume = templateConsume;
        this.templateSecondaryConsume = templateSecondaryConsume;
        this.maxNum = maxNum;
        this.maxItems = maxitems;
        this.timeout = timeout;
    }


}
