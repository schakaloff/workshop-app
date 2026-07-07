package DB;

public class ShopSettings {
    private static ShopSettings instance;
    private double pst = 0.07;
    private double gst = 0.05;

    // ─── Single source of truth for app version ──────────────────────────────────
    public static final String VERSION = "1.0.0";

    private ShopSettings() {
    }

    public static ShopSettings get() {
        if (instance == null)
            instance = new ShopSettings();
        return instance;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────────
    public double getPst() {
        return pst;
    }

    public double getGst() {
        return gst;
    }

    public void setPst(double pst) {
        this.pst = pst;
    }

    public void setGst(double gst) {
        this.gst = gst;
    }

    // ─── Tax calculation helpers ─────────────────────────────────────────────────
    public double calcPst(double subtotal, boolean hasPstNumber) {
        return hasPstNumber ? 0.0 : subtotal * pst;
    }

    public double calcGst(double subtotal, boolean hasGstNumber) {
        return hasGstNumber ? 0.0 : subtotal * gst;
    }

    public static double[] calcTaxes(double labour, double parts,
            boolean hasWarranty,
            boolean hasPstNum, boolean hasGstNum) {
        ShopSettings s = get();
        double taxBase = labour + parts;
        double pst = s.calcPst(taxBase, hasPstNum);
        double gst = s.calcGst(taxBase, hasGstNum);
        return new double[] { pst, gst };
    }
}