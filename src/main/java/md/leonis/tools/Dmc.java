package md.leonis.tools;

import boofcv.alg.color.ColorLab;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"color", "name", "red", "green", "blue"})
public class Dmc {

    private String color;
    private String name;
    private int red;
    private int green;
    private int blue;

    private double[] lab = null;

    public Dmc() {
    }

    public Dmc(String color, String name, int red, int green, int blue) {
        this.color = color;
        this.name = name;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    public String toString() {
        return "Dmc{" +
                "color='" + color + '\'' +
                ", name='" + name + '\'' +
                ", red=" + red +
                ", green=" + green +
                ", blue=" + blue +
                '}';
    }

    public double[] getLab() {
        if (lab == null) {
            lab = new double[3];
            ColorLab.rgbToLab(red, green, blue, lab);
        }
        return lab;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }
}
