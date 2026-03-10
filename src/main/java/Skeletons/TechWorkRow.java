package Skeletons;

public class TechWorkRow {
    private final int workOrderNumber;
    private final String type;
    private final String status;
    private final double labourAmount;
    private final String finishedDate;

    public TechWorkRow(int workOrderNumber, String type, String status, double labourAmount, String finishedDate) {
        this.workOrderNumber = workOrderNumber;
        this.type = type;
        this.status = status;
        this.labourAmount = labourAmount;
        this.finishedDate = finishedDate;
    }

    public int getWorkOrderNumber() {
        return workOrderNumber;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public double getLabourAmount() {
        return labourAmount;
    }

    public String getFinishedDate() {
        return finishedDate;
    }
}