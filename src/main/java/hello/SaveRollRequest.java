package hello;

public class SaveRollRequest {
    private final String roll;
    private final String color;
    private final String platform;
    private final String created;


    public SaveRollRequest(String roll, String color, String platform, String created) {
        this.roll = roll;
        this.color = color;
        this.platform = platform;
        this.created = created;
    }

    public String getCreated() {
        return created;
    }

    public String getRoll() {
        return roll;
    }

    public String getPlatform() {
        return platform;
    }

    public String getColor() {
        return color;
    }

}
