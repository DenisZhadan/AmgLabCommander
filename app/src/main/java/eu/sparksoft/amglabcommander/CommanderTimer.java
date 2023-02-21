package eu.sparksoft.amglabcommander;

public class CommanderTimer {
    public String name;
    public String address;

    public String toString() {
        return this.name;
    }

    public CommanderTimer(String name, String address) {
        this.name = name;
        this.address = address;
    }
}
