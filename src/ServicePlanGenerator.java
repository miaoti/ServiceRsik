import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ServicePlanGenerator {
    private List<String[]> services = new ArrayList<>();
    private Random random = new Random();

    public ServicePlanGenerator(String filePath) throws IOException {
        loadServices(filePath);
    }

    // Load the dataset
    private void loadServices(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        for (String line : lines) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue; // Skip comments and empty lines
            String[] data = line.split(",");
            services.add(data);
        }
    }

    // Generate service plans with dynamic serviceId starting from 1
    public List<List<Integer>> generateServicePlans(int numPlans, int maxPlanSize, boolean serviceRepeat) {
        List<List<Integer>> servicePlans = new ArrayList<>();
        Map<String, Integer> serviceIdMap = new HashMap<>();
        int currentServiceId = 1;

        for (int i = 0; i < numPlans; i++) {
            Set<String> usedServices = new HashSet<>();
            List<Integer> servicePlan = new ArrayList<>();
            int planSize = random.nextInt(maxPlanSize) + 2; // Randomly determine the size of the plan, we had a max

            while (servicePlan.size() < planSize) {
                int randomIndex = random.nextInt(services.size());
                String serviceName = services.get(randomIndex)[9]; // Service Name is the 10th attribute

                if (!serviceRepeat && usedServices.contains(serviceName)) {
                    continue; // Skip this service if it's already used
                }

                // Assign serviceId
                if (!serviceIdMap.containsKey(serviceName)) {
                    serviceIdMap.put(serviceName, currentServiceId++);
                }

                int serviceId = serviceIdMap.get(serviceName);
                servicePlan.add(serviceId);
                usedServices.add(serviceName);
            }
            servicePlans.add(servicePlan);
        }

        return servicePlans;
    }

    // Print and save the service plans
    public void saveServicePlans(List<List<Integer>> servicePlans, boolean serviceRepeat, String outputFilePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));

        for (int i = 0; i < servicePlans.size(); i++) {
            List<Integer> plan = servicePlans.get(i);
            writer.write("P" + (i + 1) + " ");
            System.out.print("P" + (i + 1) + " ");
            for (int j = 0; j < plan.size(); j++) {
                writer.write("S" + plan.get(j));
                System.out.print("S" + plan.get(j));
                if (j < plan.size() - 1) {
                    writer.write("->");
                    System.out.print("->");
                }
            }
            writer.write("\n");
            System.out.println();

            if (serviceRepeat) {
                for (int serviceId : plan) {
                    printServiceDetails(writer, serviceId);
                }
            }
        }

        // If repeat is false, output each service info only once
        if (!serviceRepeat) {
            Set<Integer> uniqueServices = new HashSet<>();
            for (List<Integer> plan : servicePlans) {
                for (int serviceId : plan) {
                    if (!uniqueServices.contains(serviceId)) {
                        printServiceDetails(writer, serviceId);
                        uniqueServices.add(serviceId);
                    }
                }
            }
        }

        writer.close();
    }

    // Print the service details
    private void printServiceDetails(BufferedWriter writer, int serviceId) throws IOException {
        for (String[] service : services) {
            if (serviceId == services.indexOf(service) + 1) { // Match serviceId based on list index
                writer.write("S" + serviceId + " ");
                System.out.print("S" + serviceId + " ");
                for (int i = 0; i < 9; i++) {
                    writer.write(service[i] + (i < 8 ? "," : ""));
                    System.out.print(service[i] + (i < 8 ? "," : ""));
                }
                writer.write("\n");
                System.out.println();
                break;
            }
        }
    }
}
