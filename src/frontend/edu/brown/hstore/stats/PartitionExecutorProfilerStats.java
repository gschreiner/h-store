package edu.brown.hstore.stats;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.voltdb.StatsSource;
import org.voltdb.VoltTable;
import org.voltdb.VoltTable.ColumnInfo;
import org.voltdb.VoltType;

import edu.brown.hstore.HStoreSite;
import edu.brown.hstore.PartitionExecutor;
import edu.brown.logging.LoggerUtil;
import edu.brown.logging.LoggerUtil.LoggerBoolean;
import edu.brown.profilers.PartitionExecutorProfiler;
import edu.brown.profilers.ProfileMeasurement;

public class PartitionExecutorProfilerStats extends StatsSource {
    private static final Logger LOG = Logger.getLogger(PartitionExecutorProfilerStats.class);
    private static final LoggerBoolean debug = new LoggerBoolean(LOG.isDebugEnabled());
    private static final LoggerBoolean trace = new LoggerBoolean(LOG.isTraceEnabled());
    static {
        LoggerUtil.attachObserver(LOG, debug, trace);
    }

    private final HStoreSite hstore_site;

    public PartitionExecutorProfilerStats(HStoreSite hstore_site) {
        super("EXECPROFILER", false);
        this.hstore_site = hstore_site;
    }
    
    @Override
    protected Iterator<Object> getStatsRowKeyIterator(boolean interval) {
        final Iterator<Integer> it = hstore_site.getLocalPartitionIds().iterator();
        return new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }
            @Override
            public Object next() {
                return it.next();
            }
            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    @Override
    protected void populateColumnSchema(ArrayList<ColumnInfo> columns) {
        super.populateColumnSchema(columns);
        
        // Make a dummy profiler just so that we can get the fields from it
        PartitionExecutorProfiler profiler = new PartitionExecutorProfiler();
        assert(profiler != null);
        
        columns.add(new VoltTable.ColumnInfo("PARTITION", VoltType.INTEGER));
        columns.add(new VoltTable.ColumnInfo("TRANSACTIONS", VoltType.BIGINT));
        for (ProfileMeasurement pm : profiler.getProfileMeasurements()) {
            columns.add(new VoltTable.ColumnInfo(pm.getType().toUpperCase(), VoltType.BIGINT));
        } // FOR
    }

    @Override
    protected synchronized void updateStatsRow(Object rowKey, Object[] rowValues) {
        Integer partition = (Integer)rowKey;
        PartitionExecutor.Debug dbg = hstore_site.getPartitionExecutor(partition).getDebugContext();
        PartitionExecutorProfiler profiler = dbg.getProfiler();
        assert(profiler != null);
        
        int offset = columnNameToIndex.get("PARTITION");
        rowValues[offset++] = partition;
        rowValues[offset++] = profiler.numTransactions;
        for (ProfileMeasurement pm : profiler.getProfileMeasurements()) {
            rowValues[offset++] = pm.getTotalThinkTime();
        } // FOR
        super.updateStatsRow(rowKey, rowValues);
    }
}