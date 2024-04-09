package hello;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Roll {
    private final String color;
    private final String roll;
    private final String created;
    private final String platform;

    private final int totalRedMoney = 0;
    private final int totalBlackMoney = 0;
    private final int totalWhiteMoney = 0;

    private final int id;

    public Roll(String color, String roll, String created, String platform, int id) {
       this.color = color;
       this.roll = roll;
       this.created = created;
       this.platform = platform;
        this.id = id;
    }

    public static Roll with(String color, String roll, String created, String platform, int id) {
        return new Roll(color, roll, created, platform, id);
    }

    @Override
    public String toString() {
        return "Roll{" +
                "color='" + color + '\'' +
                ", id='" + id + '\'' +
                ", roll='" + roll + '\'' +
                ", created='" + created + '\'' +
                ", platform='" + platform + '\'' +
                '}';
    }

    public String getPlatform() {
        return platform;
    }

    public int getId() {
        return id;
    }

    public String getColor() {
        return color;
    }

    public String getCreated() {
        return created;
    }

    public String getRoll() {
        return roll;
    }


    @JsonProperty("total_black_money")
    public int getTotalBlackMoney() {
        return totalBlackMoney;
    }

    @JsonProperty("total_red_money")
    public int getTotalRedMoney() {
        return totalRedMoney;
    }

@JsonProperty("total_white_money")
    public int getTotalWhiteMoney() {
        return totalWhiteMoney;
    }
}
