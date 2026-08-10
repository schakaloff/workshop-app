package Skeletons;

public class TechWorkRow {
    private final int workOrderNumber;
    private final String model;
    private final String status;
    private final double labourAmount;
    private final String finishedDate;

    public TechWorkRow(int workOrderNumber, String model, String status, double labourAmount, String finishedDate) {
        this.workOrderNumber = workOrderNumber;
        this.model = model;
        this.status = status;
        this.labourAmount = labourAmount;
        this.finishedDate = finishedDate;
    }

    public int getWorkOrderNumber() {
        return workOrderNumber;
    }

    public String getModel() {
        return model;
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