package edu.usc.infolab.ridesharing.dynamicpricing.optimization.optimizer;

import edu.usc.infolab.ridesharing.dynamicpricing.optimization.supplydemandanalyzer.SupplyDemandChart;
import edu.usc.infolab.ridesharing.dynamicpricing.optimization.supplydemandanalyzer.SupplyDemandChart1;

import java.io.IOException;
import java.util.*;

/**
 * Created by mohammad on 11/20/17.
 */
public class PredictiveOptimizer extends Optimizer {
    private static final Random RAND = new Random();

    private double m_preditionError = 0.;

    public PredictiveOptimizer(double[][] demands, double[] supplies, double[][][] transitions) throws IOException {
        super(demands, supplies, transitions);
    }

    public PredictiveOptimizer(double[][] demands, double[] supplies, double[][][] transitions, double error) throws IOException{
        super(demands, supplies, transitions);
        m_preditionError = error;
    }

    @Override
    public double Run() throws IOException{
        double totalRevenue = 0;
        int timeIntervalsSize = m_demands.length;
        int locationsSize = m_supplies.length;
        for (int t = 0; t < timeIntervalsSize - 1; t++) {
            double timeInstanceRevenue = 0;

            // Construct Source Supply/Demand Charts
            PriorityQueue<SupplyDemandChart> sourcePQ = new PriorityQueue<>(new Comparator<SupplyDemandChart>() {
                @Override
                public int compare(SupplyDemandChart o1, SupplyDemandChart o2) {
                    Double revRed1 = o1.revDec(1);
                    Double revRed2 = o2.revDec(1);
                    return revRed1.compareTo(revRed2);
                }
            });
            for (int i = 0; i < locationsSize; i++) {
                SupplyDemandChart1 priceAnalyzer = new SupplyDemandChart1(m_demands[t][i], m_supplies[i], i);
                sourcePQ.add(priceAnalyzer);
                //timeInstanceRevenue += priceAnalyzer.getRevenue(priceAnalyzer.getCurrentPrice());
            }

            // Construct Destination SupplyDemand charts
            PriorityQueue<SupplyDemandChart> destinationPQ = new PriorityQueue<>(new Comparator<SupplyDemandChart>() {
                @Override
                public int compare(SupplyDemandChart o1, SupplyDemandChart o2) {
                    Double revInc1 = o1.revInc(1);
                    Double revInc2 = o2.revInc(1);
                    return -1 * revInc1.compareTo(revInc2);
                }
            });

            double[] futureSupplies = getFutureSupply(sourcePQ.toArray(new SupplyDemandChart[0]), t);
            for (int i = 0; i < locationsSize; i++) {
                double rand = ((RAND.nextDouble() * 2) - 1);
                double d = m_demands[t+1][i];
                double error =  rand * m_preditionError * d;
                SupplyDemandChart1 sdc = new SupplyDemandChart1(m_demands[t+1][i] + (int)error, futureSupplies[i], i);
                destinationPQ.add(sdc);
            }

            // Compute current and max number of trips
            double[][] maxDemand = new double[locationsSize][locationsSize];
            double[][] currentTrips = new double[locationsSize][locationsSize];
            double[] srcTrips = new double[m_supplies.length];
            double[] desTrips = new double[m_supplies.length];
            for (Iterator<SupplyDemandChart> it = sourcePQ.iterator(); it.hasNext();) {
                SupplyDemandChart source = it.next();
                for (int j = 0; j < maxDemand.length; j++) {
                    maxDemand[source.getID()][j] = m_demands[t][source.getID()] * m_transitions[t][source.getID()][j];
                    currentTrips[source.getID()][j] = source.getCurrentTrips() * m_transitions[t][source.getID()][j];
                    srcTrips[source.getID()] += source.getCurrentTrips() * m_transitions[t][source.getID()][j];
                    desTrips[j] += source.getCurrentTrips() * m_transitions[t][source.getID()][j];
                }
            }

            while (!destinationPQ.isEmpty() && destinationPQ.peek().revInc(1) > sourcePQ.peek().revDec(1)) {
                SupplyDemandChart destinationTop = destinationPQ.poll();
                double revInc = destinationTop.revInc(1);
                int destID = destinationTop.getID();

                List<SupplyDemandChart> incompatibleSources = new ArrayList<>();
                SupplyDemandChart sourceTop = null;
                int sourceID = -1;
                boolean foundCompatible = false;
                while (!sourcePQ.isEmpty()) {
                    sourceTop = sourcePQ.poll();
                    double revDec = sourceTop.revDec(1);
                    if (revDec > revInc) {
                        incompatibleSources.add(sourceTop);
                        break;
                    }

                    sourceID = sourceTop.getID();
                    if (currentTrips[sourceID][destID] + 1 <= maxDemand[sourceID][destID] &&
                            srcTrips[sourceID] + 1 <= m_supplies[sourceID]) {
                        foundCompatible = true;
                        break;
                    }
                    incompatibleSources.add(sourceTop);
                }
                if (foundCompatible) {
                    //timeInstanceRevenue += (destinationTop.revInc(1) - sourceTop.revDec(1));
                    currentTrips[sourceID][destID]++;
                    srcTrips[sourceID]++;
                    desTrips[destID]++;

                    double newSourcePrice = sourceTop.getAdjustedPrice(sourceTop.getCurrentTrips() + 1);
                    sourceTop.setCurrentPrice(newSourcePrice);
                    sourcePQ.add(sourceTop);

                    destinationTop.addSupply(1);
                    destinationPQ.add(destinationTop);
                }
                for (SupplyDemandChart chart : incompatibleSources) {
                    sourcePQ.add(chart);
                }
            }

            for (Iterator<SupplyDemandChart> it = sourcePQ.iterator(); it.hasNext();) {
                SupplyDemandChart sdc = it.next();
                log(String.format("%d,%s", t, sdc.getSummary()));
                timeInstanceRevenue += sdc.getRevenue();
            }

            // (TODO): adjust future supplies
            for (int i = 0; i < m_supplies.length; i++) {
                futureSupplies[i] = m_supplies[i] - srcTrips[i] + desTrips[i];
                if (futureSupplies[i] < 0) {
                    System.out.printf("PredictiveOptimizer - Run - futureSupply: %.2f, supply: %.2f, srcTrips: %.2f, desTrips: %.2f\n", futureSupplies[i], m_supplies[i], srcTrips[i], desTrips[i]);
                }
            }
            m_supplies = futureSupplies;

            totalRevenue += timeInstanceRevenue;
        }

        // (TODO): last time instance
        double lastInstanceRevenue = 0;
        for (int i = 0; i < locationsSize; i++) {
            SupplyDemandChart1 sdc = new SupplyDemandChart1(m_demands[timeIntervalsSize-1][i], m_supplies[i], i);
            log(String.format("%d,%s", timeIntervalsSize - 1, sdc.getSummary()));
            lastInstanceRevenue += sdc.getRevenue();
        }
        totalRevenue += lastInstanceRevenue;

        return totalRevenue;
    }

    @Override
    protected String getType() {
        return "predictive";
    }

    public void setPredictionError(double error) {
        m_preditionError = error;
    }
}
