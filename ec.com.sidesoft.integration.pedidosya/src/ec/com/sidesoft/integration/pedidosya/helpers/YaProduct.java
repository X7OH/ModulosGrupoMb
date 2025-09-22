package ec.com.sidesoft.integration.pedidosya.helpers;

public class YaProduct {

	private String externalId;
	private String notaI;
    private int prodQty;
	private double price;
	
	public YaProduct(String externalId, String notaI, int prodQty, double price) {
        this.externalId = externalId;
        this.notaI = notaI;
        this.prodQty = prodQty;
        this.price = price;
    }
	
	public String getExternalId() {
        return externalId;
    }
	
	public String getNotaI() {
        return notaI;
    }

    public int getProdQty() {
        return prodQty;
    }
    
    public double getPrice() {
        return price;
    }
    
	
}
