/*
 * Copyright 2017-2019 SgrAlpha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.sgr.streamify.jdbc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.nonNull;

import io.sgr.streamify.jdbc.exceptions.StreamElementProcessingException;
import io.sgr.streamify.jdbc.exceptions.UnableToCloseResourceException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

public class StreamingHelper {

    @SuppressFBWarnings(value = "OBL_UNSATISFIED_OBLIGATION", justification = "I know what I'm doing")
    @Nonnull
    public static <T> Stream<T> queryToStream(
            @Nonnull final DataSource dataSource,
            @Nonnull final String sql, @Nullable PreparedStatementSetter pss,
            @Nonnull final RowConverter<T> converter)
            throws SQLException {
        //noinspection ConstantConditions
        checkArgument(nonNull(dataSource), "DataSource should not be null!");
        checkArgument(!isNullOrEmpty(sql), "SQL should not be null or empty string!");
        //noinspection ConstantConditions
        checkArgument(nonNull(converter), "RowConverter should not be null!");
        UncheckedCloseable closeable = null;
        try {
            // Wrap resources so we can close them later after all elements been processed not right after function returns!
            final Connection conn = dataSource.getConnection();
            closeable = UncheckedCloseable.wrap(conn);
            final PreparedStatement ps = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            if (nonNull(pss)) {
                pss.setValues(ps);
            }
            closeable = closeable.nest(ps);
            ps.setFetchSize(Integer.MIN_VALUE);
            final ResultSet rs = ps.executeQuery();
            closeable = closeable.nest(rs);
            return StreamSupport
                    .stream(new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
                        @Override
                        public boolean tryAdvance(final Consumer<? super T> action) {
                            try {
                                if (rs.next()) {
                                    action.accept(converter.read(rs));
                                    return true;
                                }
                                return false;
                            } catch (SQLException e) {
                                throw new StreamElementProcessingException(e);
                            }
                        }
                    }, false)
                    .onClose(closeable);
        } catch (SQLException e) {
            try {
                if (nonNull(closeable)) {
                    closeable.close();
                }
            } catch (Exception ex) {
                e.addSuppressed(ex);
            }
            throw e;
        }
    }

    private interface UncheckedCloseable extends Runnable, AutoCloseable {

        static UncheckedCloseable wrap(final AutoCloseable closeable) {
            return closeable::close;
        }

        @Override
        default void run() {
            try {
                close();
            } catch (Exception ex) {
                throw new UnableToCloseResourceException(ex);
            }
        }

        default UncheckedCloseable nest(final AutoCloseable closeable) {
            return () -> {
                try (UncheckedCloseable ignored = this) {
                    closeable.close();
                }
            };
        }
    }

}
