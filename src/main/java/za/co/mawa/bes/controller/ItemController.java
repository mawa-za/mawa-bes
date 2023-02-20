package za.co.mawa.bes.controller;

public class ItemController {
    private String id;
    private ItemController(String id) {
        this.id = id;
    }
    public static ItemController getInstance(String id) {
        return new ItemController(id);
    }
    public void get(){}
    public void post(){}
    public void put(){}
    public void delete(){}
}
