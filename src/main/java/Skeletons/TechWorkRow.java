package Skeletons;

public class TechWorkRow {
    private final int workOrderNumber;
    private final String status;
    private final double labourAmount;
    private final String device;

    public TechWorkRow(int workOrderNumber, String status, double labourAmount, String device) {
        this.workOrderNumber = workOrderNumber;
        this.status = status;
        this.labourAmount = labourAmount;
        this.device = device;
    }

    public int getWorkOrderNumber() {
        return workOrderNumber;
    }

    public String getStatus() {
        return status;
    }

    public double getLabourAmount() {
        return labourAmount;
    }

    public String getDevice() {
        return device;
    }
}