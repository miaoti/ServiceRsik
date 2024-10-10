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
            String outputRiskFile = "service_plan_risks.csv";

            RiskScoreCalculator riskCalculator = new RiskScoreCalculator();
            riskCalculator.calculateRiskScoresWithNormalization(servicePlansFile, outputRiskFile, serviceRepeat);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
