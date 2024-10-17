import java.io.*;
import java.util.*;

public class RiskScoreCalculator {

    private final double[] weights = {0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1}; // Assuming equal weights for simplicity, but I think this can be a manual input

    // Method to calculate risk scores based on the selected method
    public void calculateRiskScores(String inputFilePath, String outputFilePath, boolean serviceRepeat, String method, String historicalDataPath, double[] idealThresholds, double[] criticalThresholds) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));

        Map<String, String[]> serviceAttributesMap = new HashMap<>();
        List<ServicePlan> servicePlans = new ArrayList<>();

        String line;

        // Read through the file to collect service plans and service attributes
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("P")) {
                String[] planParts = line.split(" ");
                servicePlans.add(new ServicePlan(planParts[0], planParts[1]));
            } else {
                String[] parts = line.split(" ", 2);
                String serviceId = parts[0].trim();
                String[] attributes = parts[1].split(",");
                serviceAttributesMap.put(serviceId, attributes);
            }
        }

        // Determine the calculation method
        switch (method.toLowerCase()) {
            case "old":
                writer.write("Plan,ServicePath,RiskScore");
                writer.newLine();
                calculateOldMethod(servicePlans, serviceAttributesMap, serviceRepeat);
                break;
            case "historical":
                writer.write("Plan,ServicePath,H-RiskScore");
                writer.newLine();
                if (historicalDataPath == null) {
                    throw new IllegalArgumentException("Historical dataset must be provided for the historical normalization method.");
                }
                calculateHistoricalRiskScores(servicePlans, serviceAttributesMap, serviceRepeat, historicalDataPath);
                break;
            case "threshold":
                writer.write("Plan,ServicePath,T-RiskScore");
                writer.newLine();
                if (idealThresholds == null || criticalThresholds == null) {
                    throw new IllegalArgumentException("Ideal and critical thresholds must be provided for the threshold-based evaluation method.");
                }
                calculateThresholdBasedRiskScores(servicePlans, serviceAttributesMap, idealThresholds, criticalThresholds, serviceRepeat);
                break;
            default:
                throw new IllegalArgumentException("Invalid method. Choose from 'old', 'historical', or 'threshold'.");
        }

        // Write the results to the CSV file and print them
        for (ServicePlan servicePlan : servicePlans) {
            writer.write(servicePlan.getPlanName() + "," + servicePlan.getServicePath() + "," +
                    String.format("%.4f", servicePlan.getRiskScore()));
            writer.newLine();

            System.out.println(servicePlan.getPlanName() + "," + servicePlan.getServicePath() + "," +
                    String.format("%.4f", servicePlan.getRiskScore()));
        }

        reader.close();
        writer.close();
    }

    // Old method for risk score calculation
    private void calculateOldMethod(List<ServicePlan> servicePlans, Map<String, String[]> serviceAttributesMap, boolean serviceRepeat) {
        for (ServicePlan servicePlan : servicePlans) {
            List<String> servicesInPath = Arrays.asList(servicePlan.getServicePath().split("->"));
            double riskScore = calculatePlanRiskScore(servicesInPath, serviceAttributesMap, serviceRepeat);
            servicePlan.setRiskScore(riskScore);
            servicePlan.setNormalizedScore(normalizeRiskScore(servicePlans, riskScore));
        }
    }

    // Normalization step for old method
    private double normalizeRiskScore(List<ServicePlan> servicePlans, double riskScore) {
        double minRisk = servicePlans.stream().mapToDouble(ServicePlan::getRiskScore).min().orElse(0);
        double maxRisk = servicePlans.stream().mapToDouble(ServicePlan::getRiskScore).max().orElse(1);
        return (riskScore - minRisk) / (maxRisk - minRisk);
    }

    // Historical
    private void calculateHistoricalRiskScores(List<ServicePlan> servicePlans, Map<String, String[]> serviceAttributesMap, boolean serviceRepeat, String historicalDataPath) throws IOException {
        // Calculate min and max values from the historical dataset
        Map<String, double[]> minMaxValues = calculateMinMaxValues(historicalDataPath);
        double[] minValues = minMaxValues.get("min");
        double[] maxValues = minMaxValues.get("max");

        // For each service plan, calculate the normalized risk score
        for (ServicePlan servicePlan : servicePlans) {
            List<String> servicesInPath = Arrays.asList(servicePlan.getServicePath().split("->"));
            double totalRisk = 0.0;
            int serviceCount = servicesInPath.size();

            // Calculate the risk score for each service in the plan
            for (String serviceId : servicesInPath) {
                String[] serviceAttributes = serviceAttributesMap.get(serviceId);
                if (serviceAttributes == null) {
                    throw new IllegalArgumentException("Service ID " + serviceId + " not found in attributes map.");
                }

                double serviceRisk = 0.0;

                // Normalize each attribute using the historical min-max values and compute the weighted risk score
                for (int i = 0; i < 9; i++) {
                    double normalizedValue = (Double.parseDouble(serviceAttributes[i]) - minValues[i]) / (maxValues[i] - minValues[i]);
                    serviceRisk += weights[i] * normalizedValue; // Apply weights
                }

                totalRisk += serviceRisk;
            }

            // Set the average risk score
            // Average risk score for the service plan
            servicePlan.setRiskScore(totalRisk / serviceCount);
        }
    }

    private Map<String, double[]> calculateMinMaxValues(String historicalDataPath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(historicalDataPath));
        Map<String, double[]> minMaxValues = new HashMap<>();
        String line;

        double[] minValues = new double[9];
        double[] maxValues = new double[9];
        Arrays.fill(minValues, Double.MAX_VALUE);
        Arrays.fill(maxValues, Double.MIN_VALUE);

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue; // Skip comments and empty lines

            String[] parts = line.split(",");
            if (parts.length >= 9) { // Ensure we have at least 9 attributes
                for (int i = 0; i < 9; i++) {
                    double value = Double.parseDouble(parts[i]);
                    // Update min and max values for each attribute
                    if (value < minValues[i]) {
                        minValues[i] = value;
                    }
                    if (value > maxValues[i]) {
                        maxValues[i] = value;
                    }
                }
            }
        }
        reader.close();

        // Store the min and max
        minMaxValues.put("min", minValues);
        minMaxValues.put("max", maxValues);
//        System.out.println("======================================");
//        System.out.println(minValues[0]);
//        System.out.println(maxValues[0]);

        return minMaxValues;
    }


    // Threshold-based
    private void calculateThresholdBasedRiskScores(List<ServicePlan> servicePlans, Map<String, String[]> serviceAttributesMap, double[] idealThresholds, double[] criticalThresholds, boolean serviceRepeat) {
        for (ServicePlan servicePlan : servicePlans) {
            List<String> servicesInPath = Arrays.asList(servicePlan.getServicePath().split("->"));
            double totalRiskScore = 0.0;
            int serviceCount = servicesInPath.size();

            // Calculate risk score for each service in the plan
            for (String serviceId : servicesInPath) {
                double riskScore = calculateThresholdBasedServiceRisk(serviceId, serviceAttributesMap, idealThresholds, criticalThresholds);
                totalRiskScore += riskScore;
            }

            // Set the average risk score for this plan
            servicePlan.setRiskScore(totalRiskScore / serviceCount);
        }
    }

    // Calculate the risk score for a single service using threshold-based
    private double calculateThresholdBasedServiceRisk(String serviceId, Map<String, String[]> serviceAttributesMap, double[] idealThresholds, double[] criticalThresholds) {
        String[] serviceAttributes = serviceAttributesMap.get(serviceId);
        if (serviceAttributes == null) {
            throw new IllegalArgumentException("Service ID " + serviceId + " not found in attributes map.");
        }

        double riskScore = 0.0;

        // Loop through all the QoS attributes and calculate the risk score based on thresholds
        for (int i = 0; i < 9; i++) {
            double actualValue = Double.parseDouble(serviceAttributes[i]);
            double normalizedScore;

            if (i == 1 || i == 2 || i == 7) { // Non-beneficial attributes (Response Time, Latency, Throughput)
                // For non-beneficial attributes, lower values are better, so critical thresholds
                normalizedScore = 1 - (actualValue / criticalThresholds[i]); // 0 to 1, 1 is best
            } else { // Beneficial attributes (Availability, Reliability)
                // For beneficial attributes, higher values are better, so ideal thresholds
                normalizedScore = actualValue / idealThresholds[i];
            }

            // Ensure the score
            if (normalizedScore < 0) normalizedScore = 0;
            if (normalizedScore > 1) normalizedScore = 1;


            riskScore += weights[i] * normalizedScore;
        }

        return riskScore;
    }


    // Helper method for calculating the plan risk score (used in all designs)
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

    private double calculateServiceRiskScore(String serviceId, Map<String, String[]> serviceAttributesMap) {
        String[] serviceAttributes = serviceAttributesMap.get(serviceId);
        if (serviceAttributes == null) {
            throw new IllegalArgumentException("Service ID " + serviceId + " not found in attributes map.");
        }

        double riskScore = 0.0;
        for (int i = 0; i < 9; i++) {
            riskScore += weights[i] * Double.parseDouble(serviceAttributes[i]);
        }

        return riskScore;
    }

    // Helper class to represent a service plan and its risk score
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
