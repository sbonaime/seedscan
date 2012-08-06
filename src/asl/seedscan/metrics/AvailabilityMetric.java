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

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Calendar;

import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.seedsplitter.*;

public class AvailabilityMetric
extends Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.AvailabilityMetric");

    public String getName()
    {
        return "AvailabilityMetric";
    }

    public void process()
    {
        ;
    }
}

