/*
 * Copyright 2012, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */
package asl.seedscan.metrics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.metadata.Channel;
import asl.metadata.ChannelArray;
import asl.metadata.meta_new.ChannelMeta.ResponseUnits;
import asl.metadata.meta_new.ChannelMetaException;
import asl.seedscan.event.EventCMT;
import asl.util.PlotMaker2;
import asl.util.PlotMakerException;
import asl.util.Trace;
import asl.util.TraceException;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.SphericalCoords;
import edu.sc.seis.TauP.TauP_Time;

public class EventCompareStrongMotion extends Metric {
	private static final Logger logger = LoggerFactory.getLogger(asl.seedscan.metrics.EventCompareStrongMotion.class);

	private Hashtable<String, EventCMT> eventCMTs = null;

	private static final double PERIOD1 = 25;
	private static final double PERIOD2 = 20;
	private static final double PERIOD3 = 4;
	private static final double PERIOD4 = 2;

	private static final double f1 = 1. / PERIOD1;
	private static final double f2 = 1. / PERIOD2;
	private static final double f3 = 1. / PERIOD3;
	private static final double f4 = 1. / PERIOD4;

	Channel[] channels = new Channel[9];

	private double xDist = 0.;

	@Override
	public long getVersion() {
		return 1;
	}

	@Override
	public String getName() {
		return "EventCompareStrongMotion";
	}

	public void process() {

		logger.info("-Enter- [ Station {} ] [ Day {} ]", getStation(), getDay());

		eventCMTs = getEventTable();
		if (eventCMTs == null) {
			logger.info(String.format("No Event CMTs found for Day=[%s] --> Skip EventCompareStrongMotion Metric",
					getDay()));
			return;
		}

		// If station doesn't have a strongmotion sensor then don't try to
		// compute this metric:
		if (!weHaveChannels("20", "LN")) {
			logger.info(
					String.format("== Day=[%s] Stn=[%s] - metadata + data NOT found for loc=20 band=LN --> Skip Metric",
							getDay(), getStation()));
			return;
		}

		boolean compute00 = weHaveChannels("00", "LH");
		boolean compute10 = weHaveChannels("10", "LH");

		/**
		 * iDigest/ iMetric ChannelX v. ChannelY 0 channels[0] = 00-LHZ v.
		 * channels[6] = 20-LNZ 1 channels[1] = 00-LHND v. channels[7] = 20-LNND
		 * 2 channels[2] = 00-LHED v. channels[8] = 20-LNED 3 channels[3] =
		 * 10-LHZ v. channels[6] = 20-LNZ 4 channels[4] = 10-LHND v. channels[7]
		 * = 20-LNND 5 channels[5] = 10-LHED v. channels[8] = 20-LNED
		 **/
		int nDigests = 6;
		double corrVal = 0.;

		ByteBuffer[] digestArray = new ByteBuffer[nDigests];
		
		double[] results = new double[nDigests];
		boolean[] returnResults = new boolean[nDigests];

		channels[0] = new Channel("00", "LHZ");
		channels[1] = new Channel("00", "LHND");
		channels[2] = new Channel("00", "LHED");
		channels[3] = new Channel("10", "LHZ");
		channels[4] = new Channel("10", "LHND");
		channels[5] = new Channel("10", "LHED");
		channels[6] = new Channel("20", "LNZ");
		channels[7] = new Channel("20", "LNND");
		channels[8] = new Channel("20", "LNED");

		if (compute00) {
			for (int i = 0; i < 3; i++) {
				Channel channelX = channels[i];
				Channel channelY = channels[i + 6];
				ChannelArray channelArray = new ChannelArray(channelX, channelY);
				digestArray[i] = metricData.valueDigestChanged(channelArray, createIdentifier(channelX, channelY),
						getForceUpdate());
				results[i] = 0.;
			}
		}
		if (compute10) {
			for (int i = 3; i < 6; i++) {
				Channel channelX = channels[i];
				Channel channelY = channels[i + 3];
				ChannelArray channelArray = new ChannelArray(channelX, channelY);
				digestArray[i] = metricData.valueDigestChanged(channelArray, createIdentifier(channelX, channelY),
						getForceUpdate());
				results[i] = 0.;
			}
		}

		if (compute00) {
			if (digestArray[0] == null && digestArray[1] == null && digestArray[2] == null) {
				compute00 = false;
			}
		}
		if (compute10) {
			if (digestArray[3] == null && digestArray[4] == null && digestArray[5] == null) {
				compute10 = false;
			}
		}

		if (!compute00 && !compute10) {
			logger.info(String.format(
					"== Day=[%s] Stn=[%s] - digest==null (or missing)for BOTH 00-LH and 10-LH chans --> Skip Metric",
					getDay(), getStation()));
			return;
		}

		int nEvents = 0;
		int eventNumber = 0;

		// Loop over Events for this day
		try { // getZNE() method try/catch
			SortedSet<String> eventKeys = new TreeSet<String>(eventCMTs.keySet());
			for (String key : eventKeys) {

				EventCMT eventCMT = eventCMTs.get(key);

				// Window the data from the Event (PDE) Origin.
				// Use larger time window to do the instrument decons and trim
				// it down later:

				long duration = 8000000L; // 8000 sec = 8000000 msecs
				long eventStartTime = eventCMT.getTimeInMillis(); // Event
																	// origin
																	// epoch
																	// time in
																	// millisecs
				long eventEndTime = eventStartTime + duration;

				// Use P and S arrival times to trim the window down for
				// comparison:
				double[] arrivalTimes = getEventArrivalTimes(eventCMT);
				if (arrivalTimes == null) {
					logger.info(
							"== {}: arrivalTimes==null for stn=[{}] day=[{}]: Distance to stn probably > 97-deg --> Don't compute metric\n",
							getName(), getStation(), getDay());
					continue;
				}

				eventNumber++;
				int nstart = (int) (arrivalTimes[0] - 120.); // P - 120 sec
				int nend = (int) (arrivalTimes[1] + 60.); // S + 120 sec
				if (nstart < 0)
					nstart = 0;

				ResponseUnits units = ResponseUnits.DISPLACEMENT;
				ArrayList<double[]> dataDisp = new ArrayList<double[]>();

				ArrayList<double[]> dataDisp00 = null;
				if (compute00) {
					dataDisp00 = metricData.getZNE(units, "00", "LH", eventStartTime, eventEndTime, f1, f2, f3, f4);
					if (dataDisp00 != null) {
						dataDisp.addAll(dataDisp00);
					} else {
						compute00 = false;
					}
				}
				ArrayList<double[]> dataDisp10 = null;
				if (compute10) {
					dataDisp10 = metricData.getZNE(units, "10", "LH", eventStartTime, eventEndTime, f1, f2, f3, f4);
					if (dataDisp10 != null) {
						dataDisp.addAll(dataDisp10);
					} else {
						compute10 = false;
					}
				}
				ArrayList<double[]> dataDisp20 = metricData.getZNE(units, "20", "LN", eventStartTime, eventEndTime, f1,
						f2, f3, f4);
				if (dataDisp20 != null) {
					dataDisp.addAll(dataDisp20);
				}

				if ((dataDisp00 == null && dataDisp10 == null) || dataDisp20 == null) {
					logger.info("== {}: day=[{}] getZNE returned null data --> skip this event\n", getName(), getDay());
					continue;
				}

				if (getMakePlots()) {
					makePlots(dataDisp00, dataDisp10, dataDisp20, nstart, nend, key, eventNumber);
				}

				// switched from rmsDiff to scaleFac with 00/10 being the
				// reference

				if (compute00) {
					for (int i = 0; i < 3; i++) {
						corrVal = getCorr(dataDisp00.get(i), dataDisp20.get(i), nstart, nend);
						if (corrVal >= 0.85) {
							returnResults[i] = true;
							results[i] += scaleFac(dataDisp00.get(i), dataDisp20.get(i), nstart, nend);
						}
					}
				}
				if (compute10) {
					for (int i = 0; i < 3; i++) {
						corrVal = getCorr(dataDisp10.get(i), dataDisp20.get(i), nstart, nend);
						if (corrVal >= 0.85) {
							returnResults[i + 3] = true;
							results[i + 3] += scaleFac(dataDisp10.get(i), dataDisp20.get(i), nstart, nend);
						}
					}
				}

				nEvents++;

			} // eventKeys: end loop over events

			if (nEvents == 0) { // Didn't make any measurements for this station
				return;
			}

			if (compute00) {
				for (int i = 0; i < 3; i++) {
					Channel channelX = channels[i];
					Channel channelY = channels[i + 6];
					double result = results[i] / (double) nEvents;
					ByteBuffer digest = digestArray[i];
					if (digest == null)
						continue; // We don't want to try to inject a null
									// digest if that channel is not updated
					if (returnResults[i]) {
						metricResult.addResult(channelX, channelY, result, digest);
					} else {
						logger.info("station=[{}] day=[{}]: Low correlation", getStation(), getDay());
					}

				}
			}
			if (compute10) {
				for (int i = 3; i < 6; i++) {
					Channel channelX = channels[i];
					Channel channelY = channels[i + 3];
					double result = results[i] / (double) nEvents;
					ByteBuffer digest = digestArray[i];
					if (digest == null)
						continue; // We don't want to try to inject a null
									// digest if that channel is not updated

					if (returnResults[i]) {
						metricResult.addResult(channelX, channelY, result, digest);
					} else {
						logger.info("station=[{}] day=[{}]: Low correlation", getStation(), getDay());
					}
				}
			}
		} catch (ChannelMetaException e) {
			logger.error("ChannelMetaException:", e);
		} catch (MetricException e) {
			logger.error("MetricException:", e);
		} catch (PlotMakerException e) {
			logger.error("PlotMakerException:", e);
		} catch (TraceException e) {
			logger.error("TraceException:", e);
		}
	} // end process()

	private double scaleFac(double[] data1, double[] data2, int n1, int n2) {
		// if n1 < n2 or nend < data.length ...
		double numerator = 0.;
		double denominator = 0.;
		for (int i = n1; i < n2; i++) {
			numerator += (data1[i] * data2[i]);
			denominator += (data1[i] * data1[i]);
		}
		if (denominator == 0.) {
			logger.error(
					"station=[{}] day=[{}]: scaleFac: denominator==0 --> Divide by 0 --> Expect result = Infinity!",
					getStation(), getDay());
		}

		double result = numerator / denominator;
		// If the result is too large cap it at 4.
		if (result >= 4.) {
			result = 4.;
		}

		return result;
	}

	private double getCorr(double[] data1, double[] data2, int n1, int n2) {
		// This function computs the Pearson's correlation value for the two
		// time series
		if (n2 < n1) {
			logger.error("station=[{}] day=[{}]: calcDiff: n2 < n1 --> Bad window", getStation(), getDay());
			return NO_RESULT;
		}
		if (n2 >= data1.length || n2 >= data2.length) {
			logger.error(
					"station=[{}] day=[{}]: calcDiff: n2=[{}] > data1.length=[{}] and/or data2.length=[{}] --> Bad window",
					getStation(), getDay(), n2, data1.length, data2.length);
			return NO_RESULT;
		}

		// Calculate the mean of both data streams
		double data1mean = 0.;
		double data2mean = 0.;

		for (int i = n1; i < n2; i++) {
			data1mean += data1[i];
			data2mean += data2[i];

		}
		data1mean = data1mean / (double) data1.length;
		data2mean = data2mean / (double) data2.length;

		// Calculate the standard deviation of both data streams
		double std1 = 0.;
		double std2 = 0.;

		for (int i = n1; i < n2; i++) {
			std1 += (data1[i] - data1mean) * (data1[i] - data1mean);
			std2 += (data2[i] - data2mean) * (data2[i] - data2mean);

		}
		std1 = std1 / (double) data1.length;
		std2 = std2 / (double) data2.length;

		// Calculate the r correlation
		double r = 0.;
		for (int i = n1; i < n2; i++) {
			r += (data1[i] - data1mean) * (data2[i] - data2mean) / (std1 * std2);

		}
		r = r / (double) (data1.length - 1);

		return r;
	}

	private double[] getEventArrivalTimes(EventCMT eventCMT) {
		double evla = eventCMT.getLatitude();
		double evlo = eventCMT.getLongitude();
		double evdep = eventCMT.getDepth();
		double stla = stationMeta.getLatitude();
		double stlo = stationMeta.getLongitude();
		double gcarc = SphericalCoords.distance(evla, evlo, stla, stlo);
		xDist = gcarc;
		double azim = SphericalCoords.azimuth(evla, evlo, stla, stlo);
		TauP_Time timeTool = null;
		try {
			timeTool = new TauP_Time("prem");
			timeTool.parsePhaseList("P,S");
			timeTool.depthCorrect(evdep);
			timeTool.calculate(gcarc);
		} catch (Exception e) {
			logger.error("Exception:", e);
		}

		List<Arrival> arrivals = timeTool.getArrivals();

		// We could screen by max distance (e.g., 90 deg for P direct)
		// or by counting arrivals (since you won't get a P arrival beyond about
		// 97 deg or so)
		if (arrivals.size() != 2) { // Either we don't have both P & S or we
									// don't have just P & S
			logger.info(String.format("Expected P and/or S arrival times not found [gcarc=%8.4f]", gcarc));
			return null;
		}

		double arrivalTimeP = 0.;
		if (arrivals.get(0).getName().equals("P")) {
			arrivalTimeP = arrivals.get(0).getTime();
		} else {
			logger.info(String.format("Expected P arrival time not found"));
		}
		double arrivalTimeS = 0.;
		if (arrivals.get(1).getName().equals("S")) {
			arrivalTimeS = arrivals.get(1).getTime();
		} else {
			logger.info(String.format("Expected S arrival time not found"));
		}

		logger.info(String.format(
				"Event:%s <evla,evlo> = <%.2f, %.2f> Station:%s <%.2f, %.2f> gcarc=%.2f azim=%.2f tP=%.3f tS=%.3f\n",
				eventCMT.getEventID(), evla, evlo, getStation(), stla, stlo, gcarc, azim, arrivalTimeP, arrivalTimeS));

		double[] arrivalTimes = new double[2];
		arrivalTimes[0] = arrivalTimeP;
		arrivalTimes[1] = arrivalTimeS;

		return arrivalTimes;
	}

	public void makePlots(ArrayList<double[]> d00, ArrayList<double[]> d10, ArrayList<double[]> d20, int nstart,
			int nend, String key, int eventNumber) throws PlotMakerException, TraceException {

		PlotMaker2 plotMaker = null;
		final String plotTitle = String.format("[ Event: %s ] [ Station: %s ] [ Dist: %.2f ] %s", key, getStation(),
				xDist, getName());

		final String pngName = String.format("%s.strongmtn.ev-%d.png", getOutputDir(), eventNumber);

		if (plotMaker == null) {
			plotMaker = new PlotMaker2(plotTitle);
			plotMaker.initialize3Panels("LHZ", "LH1/LHN", "LH2/LHE");
		}

		BasicStroke stroke = new BasicStroke(2.0f);

		int npts = nend - nstart + 1;

		double[] xsecs = new double[npts];
		for (int k = 0; k < xsecs.length; k++) {
			xsecs[k] = (float) (k + nstart); // hard-wired for LH? dt=1.0
		}

		try {
			if (d00 != null) {
				for (int i = 0; i < d00.size(); i++) {
					double[] dataIn = d00.get(i);
					double[] dataOut = new double[npts];
					System.arraycopy(dataIn, nstart, dataOut, 0, npts);
					plotMaker.addTraceToPanel(new Trace(xsecs, dataOut, channels[i].toString(), Color.green, stroke),
							i);
				}
			}
			if (d10 != null) {
				for (int i = 0; i < d10.size(); i++) {
					double[] dataIn = d10.get(i);
					double[] dataOut = new double[npts];
					System.arraycopy(dataIn, nstart, dataOut, 0, npts);
					plotMaker.addTraceToPanel(new Trace(xsecs, dataOut, channels[i + 3].toString(), Color.red, stroke),
							i);
				}
			}
			if (d20 != null) {
				for (int i = 0; i < d20.size(); i++) {
					double[] dataIn = d20.get(i);
					double[] dataOut = new double[npts];
					System.arraycopy(dataIn, nstart, dataOut, 0, npts);
					plotMaker.addTraceToPanel(
							new Trace(xsecs, dataOut, channels[i + 6].toString(), Color.black, stroke), i);
				}
			}
			plotMaker.writePlot(pngName);
		} catch (PlotMakerException e) {
			throw e;
		} catch (TraceException e) {
			throw e;
		}
	}
}
