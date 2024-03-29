package com.conveyal.gtfs.model;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.error.GeneralError;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Iterator;

public class FeedInfo extends Entity implements Cloneable {

    private static final long serialVersionUID = 8718856987299076452L;
    public String    feed_id = "NONE";
    public String    feed_publisher_name;
    public URL       feed_publisher_url;
    public String    feed_lang;
    public LocalDate feed_start_date;
    public LocalDate feed_end_date;
    public String    feed_version;

    public FeedInfo clone () {
        try {
            return (FeedInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Loader extends Entity.Loader<FeedInfo> {

        public Loader(GTFSFeed feed) {
            super(feed, "feed_info");
        }

        @Override
        protected boolean isRequired() {
            return false;
        }

        @Override
        public void loadOneRow() throws IOException {
            FeedInfo fi = new FeedInfo();
            fi.sourceFileLine = row;
            fi.feed_id = getStringField("feed_id", false);
            fi.feed_publisher_name = getStringField("feed_publisher_name", true);
            fi.feed_publisher_url = getUrlField("feed_publisher_url", true);
            fi.feed_lang = getStringField("feed_lang", true);
            fi.feed_start_date = getDateField("feed_start_date", false);
            fi.feed_end_date = getDateField("feed_end_date", false);
            fi.feed_version = getStringField("feed_version", false);
            if (feed.feedInfo.isEmpty()) {
                feed.feedInfo.put("NONE", fi);
                feed.feedId = fi.feed_id;
            } else {
                feed.errors.add(new GeneralError(tableName, row, null, "FeedInfo contains more than one record."));
            }
        }
    }

    public static class Writer extends Entity.Writer<FeedInfo> {

        public Writer(GTFSFeed feed) {
            super(feed, "feed_info");
        }

        @Override
        public void writeHeaders() throws IOException {
            writer.writeRecord(new String[] {"feed_id", "feed_publisher_name", "feed_publisher_url", "feed_lang",
                    "feed_start_date", "feed_end_date", "feed_version"});
        }

        @Override
        public void writeOneRow(FeedInfo i) throws IOException {
            writeStringField(i.feed_id != null && i.feed_id.equals("NONE") ? "" : i.feed_id);
            writeStringField(i.feed_publisher_name);
            writeUrlField(i.feed_publisher_url);
            writeStringField(i.feed_lang);

            if (i.feed_start_date != null) writeDateField(i.feed_start_date);
            else writeStringField("");

            if (i.feed_end_date != null) writeDateField(i.feed_end_date);
            else writeStringField("");

            writeStringField(i.feed_version);
            endRecord();
        }

        @Override
        public Iterator<FeedInfo> iterator() {
            return feed.feedInfo.values().iterator();
        }
    }
}
