import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            String filePath = "src/resource/qws2.txt";
            String outputFilePath = "service_plans.csv";

            ServicePlanGenerator generator = new ServicePlanGenerator(filePath);

            int numPlans = 5;
            int maxPlanSize = 4;
            boolean serviceRepeat = false;

            // Generate service plans
            List<List<Integer>> servicePlans = generator.generateServicePlans(numPlans, maxPlanSize, serviceRepeat);
            generator.saveServicePlans(servicePlans, serviceRepeat, outputFilePath);

            String servicePlansFile = "service_plans.csv";
            String outputRiskFile;

            // Example for each method:

            // Example: Old (From the paper)
            System.out.println("Running Old Method...");
            outputRiskFile = "service_plan_risks_old.csv";
            RiskScoreCalculator riskCalculator = new RiskScoreCalculator();
            riskCalculator.calculateRiskScores(servicePlansFile, outputRiskFile, serviceRepeat, "old", null, null, null);

            // Example: Historical
            System.out.println("Running Historical Data Method...");
            outputRiskFile = "service_plan_risks_historical.csv";
            String historicalDataPath = "src/resource/qws2.txt";
            riskCalculator.calculateRiskScores(servicePlansFile, outputRiskFile, serviceRepeat, "historical", historicalDataPath, null, null);

            // Example: Threshold-Based
            System.out.println("Running Threshold-Based Method...");
            outputRiskFile = "service_plan_risks_threshold.csv";
            // Manual Input
            double[] idealThresholds = {100, 100, 100, 100, 100, 100, 100, 0, 100};
            double[] criticalThresholds = {90, 500, 200, 90, 90, 90, 90, 500, 90};
            riskCalculator.calculateRiskScores(servicePlansFile, outputRiskFile, serviceRepeat, "threshold", null, idealThresholds, criticalThresholds);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
