package edu.usc.infolab.ridesharing.algorithms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import edu.usc.infolab.ridesharing.Status;
import edu.usc.infolab.ridesharing.Time;
import edu.usc.infolab.ridesharing.Utils;
import edu.usc.infolab.ridesharing.auction.AuctionDriver;
import edu.usc.infolab.ridesharing.auction.AuctionRequest;
import edu.usc.infolab.ridesharing.auction.Bid;

public abstract class AuctionAlgorithm<D extends AuctionDriver> extends Algorithm<AuctionRequest, D> {
	public Double profit;
	
	/**
	 * 
	 * @param distance
	 * @param time
	 * @return default fare
	 */
	public static Double FARE(Double distance, int time) {
		return 5 + (2. * distance);
	}

	public AuctionAlgorithm(Time startTime, int ati) {
		super(startTime, ati);
		profit = 0.;
	}

	/**
	 * Bid the request independently from each driver
	 */
	@Override
	public Status ProcessRequest(AuctionRequest r, Time time) {
		ArrayList<D> potentialDrivers = GetPotentialDrivers(r);
		r.stats.potentialDrivers = potentialDrivers.size();
		ArrayList<Bid> bids = new ArrayList<Bid>();
		HashMap<Integer, ArrayList<Integer>> bidTimes = new HashMap<Integer, ArrayList<Integer>>();
		// select the best bid
		int maxBidComputation = Utils.Min_Integer;
		ArrayList<Integer> badDrivers = new ArrayList<Integer>();
		for (AuctionDriver d : potentialDrivers) {
			if (!bidTimes.containsKey(d._schedule.size())) {
				bidTimes.put(d._schedule.size(), new ArrayList<Integer>());
			}
			Calendar start = Calendar.getInstance();
			// insert request into one driver's schedule
			Bid bid = d.ComputeBid(r, time);
			Calendar end = Calendar.getInstance();
			bids.add(bid);
			
			int bidTimeMillis = (int)(end.getTimeInMillis() - start.getTimeInMillis());
			bidTimes.get(d._schedule.size()).add(bidTimeMillis);
			
			if (bidTimeMillis > 10) {
				Calendar s = Calendar.getInstance();
				d.ComputeBid(r, time);
				Calendar e = Calendar.getInstance();
				int bidTimeMillis2 = (int)(e.getTimeInMillis() - s.getTimeInMillis());
				if (bidTimeMillis2 < bidTimeMillis) {
					bidTimeMillis = bidTimeMillis2;
				}
			}
			
			// track the computation time, i.e., the maximal time of one auction driver
			if (bidTimeMillis > maxBidComputation) {
				maxBidComputation = bidTimeMillis;
			}
		}
		r.stats.schedulingTime = maxBidComputation;
		
		Time start = new Time();
		// select the bids based on the auction algorithm
		AuctionDriver selectedDriver = SelectWinner(r, bids);
		Time end = new Time();
		r.stats.assignmentTime = end.SubtractInMillis(start);
		if (selectedDriver == null) {
			return Status.NOT_ASSIGNED;
		}
		selectedDriver.AddRequest(r, time);
		return Status.ASSIGNED;
	}
	
	//@Override
	//protected D GetNewDriver() {
	//	return (D)AuctionInput.GetNewDriver();
	//}

	public abstract AuctionDriver SelectWinner(AuctionRequest r, ArrayList<Bid> bids);

}
