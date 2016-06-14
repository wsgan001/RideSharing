package edu.usc.infolab.ridesharing.launcher;

import edu.usc.infolab.ridesharing.Time;
import edu.usc.infolab.ridesharing.Utils;
import edu.usc.infolab.ridesharing.algorithms.KineticTreeAlgorithm;
import edu.usc.infolab.ridesharing.algorithms.NearestNeighborAlgorithm;
import edu.usc.infolab.ridesharing.algorithms.SecondPriceAuctionAlgorithm;
import edu.usc.infolab.ridesharing.algorithms.SecondPriceAuctionWithReservedValueAlgorithm;
import edu.usc.infolab.ridesharing.auction.AuctionDriver;
import edu.usc.infolab.ridesharing.auction.AuctionRequest;
import edu.usc.infolab.ridesharing.datasets.real.nyctaxi.AuctionInput;
import edu.usc.infolab.ridesharing.datasets.real.nyctaxi.KTInput;
import edu.usc.infolab.ridesharing.kinetictree.KTDriver;
import edu.usc.infolab.ridesharing.kinetictree.KTRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class RideSharingLauncher {

	public static void main(String[] args) {
		File requestsFile = new File("../Data/trips_2013_05_12.csv");
		File driversFile = new File("../Data/drivers_2013_05_12.csv");
		StringBuilder summaries = new StringBuilder();		
		
		/*Utils.MaxWaitTime = 30;
		Utils.NumberOfVehicles = 500;
        Utils.MaxPassengers = 4;
		RunNearestNeighbor(requestsFile, driversFile);*/
		
		
		int[] maxWaitTimes = new int[]{5, 10, 15, 20, 30, 60};
        int[] numOfVehicles = new int[]{50, 100, 250, 500, 1000};
        int[] numOfPassengers = new int[]{3, 4, 5, 6, 7};
        
		Utils.NumberOfVehicles = 500;
		Utils.MaxPassengers = 4;
		for (int maxWaitTime : maxWaitTimes) {
		  Utils.MaxWaitTime = maxWaitTime;
		  System.out.println(String.format("Starting: "
              + "MaxWaitTime: %d, Number of Vehicles: %d, Max Passenger: %d\n",
              Utils.MaxWaitTime, Utils.NumberOfVehicles, Utils.MaxPassengers));
		  summaries.append(RunAlgorithms(requestsFile, driversFile));		  
		}
		
		Utils.MaxWaitTime = 15;
        Utils.MaxPassengers = 4;
		for (int numOfVehicle : numOfVehicles) {
		  Utils.NumberOfVehicles = numOfVehicle;
		  System.out.println(String.format("Starting: "
              + "MaxWaitTime: %d, Number of Vehicles: %d, Max Passenger: %d\n",
              Utils.MaxWaitTime, Utils.NumberOfVehicles, Utils.MaxPassengers));
		  summaries.append(RunAlgorithms(requestsFile, driversFile));
		}
		
		Utils.MaxWaitTime = 15;
        Utils.NumberOfVehicles = 500;
        for (int numOfPassenger : numOfPassengers) {
		  Utils.MaxPassengers = numOfPassenger;
		  System.out.println(String.format("Starting: "
              + "MaxWaitTime: %d, Number of Vehicles: %d, Max Passenger: %d\n",
              Utils.MaxWaitTime, Utils.NumberOfVehicles, Utils.MaxPassengers));
		  summaries.append(RunAlgorithms(requestsFile, driversFile));
		}
        
        String finalSummary = summaries.toString();
		System.out.println(finalSummary);
		try {
		  File oFile = new File(String.format("Summaries_%s.csv", 
		      Time.sdf.format(Calendar.getInstance().getTime())));
		  FileWriter fw = new FileWriter(oFile);
		  BufferedWriter bw = new BufferedWriter(fw);
		  bw.write(finalSummary);
		  bw.close();
		  fw.close();
		} catch (IOException ioe) {
		  ioe.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	private static String RunAlgorithms(File requestsFile, File driversFile) {
	  StringBuilder summaries = new StringBuilder();
	  summaries.append(RunSecondPriceAuction(requestsFile, driversFile));
	  summaries.append(RunFirstPriceAuction(requestsFile, driversFile));
	  summaries.append(RunNearestNeighbor(requestsFile, driversFile));
      summaries.append(RunKineticTree(requestsFile, driversFile));          
      return summaries.toString();
	}
	
	private static String RunSecondPriceAuction(File requestsFile, File driversFile) {
	  ArrayList<AuctionRequest> auctionRequests = AuctionInput.GenerateRequests(requestsFile);
      ArrayList<AuctionDriver> auctionDrivers = AuctionInput.GenerateDrivers(driversFile, Utils.NumberOfVehicles);
      Time startTime = auctionRequests.get(0).requestTime.clone();
      
      SecondPriceAuctionAlgorithm spaAlgo = new SecondPriceAuctionAlgorithm(startTime, 1);
      return String.format("%d,%d,%d,%s,%s\n",
          Utils.MaxWaitTime,
          Utils.NumberOfVehicles,
          Utils.MaxPassengers,
          spaAlgo.GetName(),
          spaAlgo.Run(auctionRequests, auctionDrivers));
	}
	
	private static String RunFirstPriceAuction(File requestsFile, File driversFile) {
	  ArrayList<AuctionRequest> auctionRequests = AuctionInput.GenerateRequests(requestsFile);
      ArrayList<AuctionDriver> auctionDrivers = AuctionInput.GenerateDrivers(driversFile, Utils.NumberOfVehicles);
      Time startTime = auctionRequests.get(0).requestTime.clone();
      
      SecondPriceAuctionWithReservedValueAlgorithm sparvAlgo = new SecondPriceAuctionWithReservedValueAlgorithm(startTime, 1);
      return String.format("%d,%d,%d,%s,%s\n",
          Utils.MaxWaitTime,
          Utils.NumberOfVehicles,
          Utils.MaxPassengers,
          sparvAlgo.GetName(),
          sparvAlgo.Run(auctionRequests, auctionDrivers));

	}
	
	private static String RunNearestNeighbor(File requestsFile, File driversFile) {
	  ArrayList<AuctionRequest> auctionRequests = AuctionInput.GenerateRequests(requestsFile);
      ArrayList<AuctionDriver> auctionDrivers = AuctionInput.GenerateDrivers(driversFile, Utils.NumberOfVehicles);
      Time startTime = auctionRequests.get(0).requestTime.clone();
      
      NearestNeighborAlgorithm nnAlgo = new NearestNeighborAlgorithm(startTime, 1);
      return String.format("%d,%d,%d,%s,%s\n",
          Utils.MaxWaitTime,
          Utils.NumberOfVehicles,
          Utils.MaxPassengers,
          nnAlgo.GetName(),
          nnAlgo.Run(auctionRequests, auctionDrivers));
	}
	
	private static String RunKineticTree(File requestsFile, File driversFile) {
	  ArrayList<KTRequest> ktRequests = KTInput.GenerateRequests(requestsFile);
      ArrayList<KTDriver> ktDrivers = KTInput.GenerateDrivers(driversFile, Utils.NumberOfVehicles);
      Time startTime = ktRequests.get(0).requestTime.clone();
      
      KineticTreeAlgorithm ktAlgo = new KineticTreeAlgorithm(startTime, 1);
      return String.format("%d,%d,%d,%s,%s\n",
          Utils.MaxWaitTime,
          Utils.NumberOfVehicles,
          Utils.MaxPassengers,ktAlgo.GetName(),
          ktAlgo.Run(ktRequests, ktDrivers));
	}

}