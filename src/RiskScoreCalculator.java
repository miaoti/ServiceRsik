import java.io.*;
import java.util.*;

public class RiskScoreCalculator {

    private final double[] weights = {0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1}; // Assuming equal weights for simplicity

    public void calculateRiskScoresWithNormalization(String inputFilePath, String outputFilePath, boolean serviceRepeat) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));

        // Write the CSV header to the output file
        writer.write("Plan,ServicePath,RiskScore,NormalizedScore");
        writer.newLine();

        // Print the CSV header to the console
        System.out.println("Plan,ServicePath,RiskScore,NormalizedScore");

        Map<String, String[]> serviceAttributesMap = new HashMap<>();
        List<ServicePlan> servicePlans = new ArrayList<>();

        String line;

        // First, read through the file to collect service plans and service attributes
        while ((line = reader.readLine()) != null) {
            line = line.trim(); // Trim whitespace
            if (line.startsWith("P")) { // Detect service plan lines since starts with P
                String[] planParts = line.split(" ");
                servicePlans.add(new ServicePlan(planParts[0], planParts[1]));
            } else { // Process service attributes lines, starts with [1], [0] is the service id
                String[] parts = line.split(" ", 2); // Split on the first space
                String serviceId = parts[0].trim();
                String[] attributes = parts[1].split(",");
                serviceAttributesMap.put(serviceId, attributes);
            }
        }

        //Calculate the risk scores for each service plan
        for (ServicePlan servicePlan : servicePlans) {
            List<String> servicesInPath = Arrays.asList(servicePlan.getServicePath().split("->"));
            double riskScore = calculatePlanRiskScore(servicesInPath, serviceAttributesMap, serviceRepeat);
            servicePlan.setRiskScore(riskScore);
        }

        //Calculate the normalized risk scores using normalization in the paper, find min and max
        double minRisk = servicePlans.stream().mapToDouble(ServicePlan::getRiskScore).min().orElse(0);
        double maxRisk = servicePlans.stream().mapToDouble(ServicePlan::getRiskScore).max().orElse(1);

        for (ServicePlan servicePlan : servicePlans) {
            double normalizedScore = (servicePlan.getRiskScore() - minRisk) / (maxRisk - minRisk);
            servicePlan.setNormalizedScore(normalizedScore);
        }

        //Write the results to the CSV file and print out
        for (ServicePlan servicePlan : servicePlans) {
            writer.write(servicePlan.getPlanName() + "," + servicePlan.getServicePath() + "," +
                    String.format("%.4f", servicePlan.getRiskScore()) + "," +
                    String.format("%.4f", servicePlan.getNormalizedScore()));
            writer.newLine();

            System.out.println(servicePlan.getPlanName() + "," + servicePlan.getServicePath() + "," +
                    String.format("%.4f", servicePlan.getRiskScore()) + "," +
                    String.format("%.4f", servicePlan.getNormalizedScore()));
        }

        reader.close();
        writer.close();
    }

    // Calculate the risk score for a service plan
    private double calculatePlanRiskScore(List<String> servicesInPath, Map<String, String[]> serviceAttributesMap, boolean serviceRepeat) {
        Set<String> uniqueServices = new HashSet<>();
        double totalRisk = 0.0;
        int count = 0;

        for (String serviceId : servicesInPath) {
            serviceId = serviceId.trim();
            if (!serviceRepeat && uniqueServices.contains(serviceId)) {
                continue; // Skip repeated services if repeats are not allowed
            }

            uniqueServices.add(serviceId);
            double serviceRisk = calculateServiceRiskScore(serviceId, serviceAttributesMap);
            totalRisk += serviceRisk;
            count++;
        }

        return totalRisk / count;
    }

    // Calculate the risk score for a single service
    private double calculateServiceRiskScore(String serviceId, Map<String, String[]> serviceAttributesMap) {
        String[] serviceAttributes = serviceAttributesMap.get(serviceId);
        if (serviceAttributes == null) {
            throw new IllegalArgumentException("Service ID " + serviceId + " not found in attributes map.");
        }

        double riskScore = 0.0;
        for (int i = 0; i < 9; i++) { // Only use the first 9
            riskScore += weights[i] * Double.parseDouble(serviceAttributes[i]);
        }

        return riskScore;
    }

    private static class ServicePlan {
        private String planName;
        private String servicePath;
        private double riskScore;
        private double normalizedScore;

        public ServicePlan(String planName, String servicePath) {
            this.planName = planName;
            this.servicePath = servicePath;
        }

        public String getPlanName() {
            return planName;
        }

        public String getServicePath() {
            return servicePath;
        }

        public double getRiskScore() {
            return riskScore;
        }

        public void setRiskScore(double riskScore) {
            this.riskScore = riskScore;
        }

        public double getNormalizedScore() {
            return normalizedScore;
        }

        public void setNormalizedScore(double normalizedScore) {
            this.normalizedScore = normalizedScore;
        }
    }
}
