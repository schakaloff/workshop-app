package DB;

public class ShopSettings {
    private static ShopSettings instance;
    private double pst = 0.07;
    private double gst = 0.05;

    // ─── Single source of truth for app version ──────────────────────────────────
    public static final String VERSION = "1.2.3";

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

    /**
     * The one place a work order's billable amounts are decided.
     * Warranty (vendor) job: only the lines the vendor is set to pay are billed at all,
     * and PST/GST are calculated only if the vendor pays them, on the billed lines.
     * Regular job: everything is billed; a customer PST/GST number removes that tax.
     */
    public static Bill calcBill(double labour, double parts, Skeletons.Vendor vendor,
            boolean hasPstNum, boolean hasGstNum) {
        ShopSettings s = get();
        if (vendor != null) {
            double l = vendor.isPaysLabour() ? labour : 0;
            double p = vendor.isPaysParts() ? parts : 0;
            double base = l + p;
            return new Bill(l, p,
                    vendor.isPaysPst() ? base * s.pst : 0.0,
                    vendor.isPaysGst() ? base * s.gst : 0.0);
        }
        double base = labour + parts;
        return new Bill(labour, parts, s.calcPst(base, hasPstNum), s.calcGst(base, hasGstNum));
    }

    public static final class Bill {
        public final double labour, parts, pst, gst;

        Bill(double labour, double parts, double pst, double gst) {
            this.labour = labour;
            this.parts = parts;
            this.pst = pst;
            this.gst = gst;
        }

        public double total() {
            return labour + parts + pst + gst;
        }
    }
}
