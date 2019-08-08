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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sgr.streamify.jdbc.exceptions.StreamElementProcessingException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

@RunWith(MockitoJUnitRunner.class)
public class StreamingHelperTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement ps;
    @Mock
    private ResultSet rs;

    @Test
    public void testIfResourcesBeenClosed() throws SQLException {
        final String sql = "SELECT col FROM some_table";
        final String columnName = "col";
        final String idValue = "some_value";

        when(dataSource.getConnection()).thenReturn(connection);
        //noinspection MagicConstant
        when(connection.prepareStatement(eq(sql), eq(ResultSet.TYPE_FORWARD_ONLY), eq(ResultSet.CONCUR_READ_ONLY))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true).thenReturn(false);
        when(rs.getString(eq(columnName))).thenReturn(idValue);
        try (
                Stream<String> stream = StreamingHelper.queryToStream(dataSource, sql, null, rs -> rs.getString(columnName))
        ) {
            assertTrue(stream.allMatch(idValue::equals));
        }

        verify(rs, times(1)).close();
        verify(ps, times(1)).close();
        verify(connection, times(1)).close();
    }

    @Test
    public void testExceptionWhenConvertingRow() throws SQLException {
        final String sql = "SELECT col FROM some_table";

        when(dataSource.getConnection()).thenReturn(connection);
        //noinspection MagicConstant
        when(connection.prepareStatement(eq(sql), eq(ResultSet.TYPE_FORWARD_ONLY), eq(ResultSet.CONCUR_READ_ONLY))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true).thenReturn(false);
        try (
                Stream<String> stream = StreamingHelper.queryToStream(dataSource, sql, null, new ExceptionOnlyRowConverter<>())
        ) {
            //noinspection ResultOfMethodCallIgnored
            stream.count();
            fail("Expecting StreamElementProcessingException here!");
        } catch (StreamElementProcessingException e) {
            assertTrue(e.getCause() instanceof SQLException);
        }

        verify(rs, times(1)).close();
        verify(ps, times(1)).close();
        verify(connection, times(1)).close();
    }

    @Test
    public void testExceptionWhenReadingResultSet() throws SQLException {
        final String sql = "SELECT col FROM some_table";

        when(dataSource.getConnection()).thenReturn(connection);
        //noinspection MagicConstant
        when(connection.prepareStatement(eq(sql), eq(ResultSet.TYPE_FORWARD_ONLY), eq(ResultSet.CONCUR_READ_ONLY))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("Something was wrong!"));
        try (
                Stream<String> stream = StreamingHelper.queryToStream(dataSource, sql, null, new ExceptionOnlyRowConverter<>())
        ) {
            //noinspection ResultOfMethodCallIgnored
            stream.count();
            fail("Expecting StreamElementProcessingException here!");
        } catch (StreamElementProcessingException e) {
            assertTrue(e.getCause() instanceof SQLException);
        }

        verify(rs, times(1)).close();
        verify(ps, times(1)).close();
        verify(connection, times(1)).close();
    }

    @Test
    public void testExceptionWhenExecutingQuery() throws SQLException {
        final String sql = "SELECT col FROM some_table";

        when(dataSource.getConnection()).thenReturn(connection);
        //noinspection MagicConstant
        when(connection.prepareStatement(eq(sql), eq(ResultSet.TYPE_FORWARD_ONLY), eq(ResultSet.CONCUR_READ_ONLY))).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("Something was wrong!"));
        try (
                Stream<String> stream = StreamingHelper.queryToStream(dataSource, sql, null, new ExceptionOnlyRowConverter<>())
        ) {
            //noinspection ResultOfMethodCallIgnored
            stream.count();
            fail("Expecting SQLException here!");
        } catch (SQLException e) {
            // Ignore
        }

        verify(ps, times(1)).close();
        verify(connection, times(1)).close();
    }

    @Test
    public void testExceptionWhenExecutingQueryAndCloseResource() throws SQLException {
        final String sql = "SELECT col FROM some_table";

        when(dataSource.getConnection()).thenReturn(connection);
        //noinspection MagicConstant
        when(connection.prepareStatement(eq(sql), eq(ResultSet.TYPE_FORWARD_ONLY), eq(ResultSet.CONCUR_READ_ONLY))).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("Something was wrong when querying!"));
        doThrow(new SQLException("Something was wrong when closing resource")).when(ps).close();
        try (
                Stream<String> stream = StreamingHelper.queryToStream(dataSource, sql, null, new ExceptionOnlyRowConverter<>())
        ) {
            //noinspection ResultOfMethodCallIgnored
            stream.count();
            fail("Expecting SQLException here!");
        } catch (SQLException e) {
            // Ignore
        }

        verify(ps, times(1)).close();
        verify(connection, times(1)).close();
    }

    private static class ExceptionOnlyRowConverter<T> implements RowConverter<T> {
        @Nullable
        @Override
        public T read(@Nonnull final ResultSet resultSet) throws SQLException {
            throw new SQLException("Something wrong when converting row!");
        }
    }

}