package Skeletons;

public class WorkOrder {

    // ─── FIELDS ─────────────────────────────────────────────────────────────────

    private final int    workorderNumber;
    private String       status;
    private String       type;
    private final String createdAt;
    private String       vendorId;
    private String       warrantyNumber;
    private String       model;
    private String       serialNumber;
    private String       problemDesc;
    private final int    customerId;
    private final double depositAmount;

    // set after construction
    private int    techId;
    private String techUsername;
    private String customerName;
    private String location;
    private String poNumber;
    private String repairType;

    // ─── CONSTRUCTOR ────────────────────────────────────────────────────────────

    public WorkOrder(int    workorderNumber,
                     String status,
                     String type,
                     String createdAt,
                     String vendorId,
                     String warrantyNumber,
                     String model,
                     String serialNumber,
                     String problemDesc,
                     int    customerId,
                     double depositAmount) {

        this.workorderNumber = workorderNumber;
        this.status          = status;
        this.type            = type;
        this.createdAt       = createdAt;
        this.vendorId        = vendorId;
        this.warrantyNumber  = warrantyNumber;
        this.model           = model;
        this.serialNumber    = serialNumber;
        this.problemDesc     = problemDesc;
        this.customerId      = customerId;
        this.depositAmount   = depositAmount;

        this.techId       = 0;
        this.techUsername = "";
        this.customerName = "";
        this.location     = "";
        this.poNumber     = "";
        this.repairType   = "";
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────────

    public int    getWorkorderNumber() { return workorderNumber; }
    public String getStatus()          { return status; }
    public String getType()            { return type; }
    public String getCreatedAt()       { return createdAt; }
    public String getVendorId()        { return vendorId; }
    public String getWarrantyNumber()  { return warrantyNumber; }
    public String getModel()           { return model; }
    public String getSerialNumber()    { return serialNumber; }
    public String getProblemDesc()     { return problemDesc; }
    public int    getCustomerId()      { return customerId; }
    public double getDepositAmount()   { return depositAmount; }
    public int    getTechId()          { return techId; }
    public String getTechUsername()    { return techUsername != null ? techUsername : ""; }
    public String getCustomerName()    { return customerName; }
    public String getLocation()        { return location; }
    public String getPoNumber()        { return poNumber; }
    public String getRepairType()      { return repairType; }

    // ─── SETTERS ────────────────────────────────────────────────────────────────

    public void setStatus(String status)                { this.status = status; }
    public void setType(String type)                    { this.type = type; }
    public void setVendorId(String vendorId)            { this.vendorId = vendorId; }
    public void setWarrantyNumber(String warrantyNumber){ this.warrantyNumber = warrantyNumber; }
    public void setModel(String model)                  { this.model = model; }
    public void setSerialNumber(String serialNumber)    { this.serialNumber = serialNumber; }
    public void setProblemDesc(String problemDesc)      { this.problemDesc = problemDesc; }
    public void setTechId(int techId)                   { this.techId = techId; }
    public void setTechUsername(String name)            { this.techUsername = name != null ? name : ""; }
    public void setCustomerName(String customerName)    { this.customerName = customerName; }
    public void setLocation(String location)            { this.location = location != null ? location : ""; }
    public void setPoNumber(String poNumber)            { this.poNumber = poNumber != null ? poNumber : ""; }
    public void setRepairType(String repairType)         { this.repairType = repairType != null ? repairType : ""; }
}