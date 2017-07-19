package com.github.mike10004.seleniumhelp;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of artifact versioning.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@SuppressWarnings("unused")
public class Version implements Comparable<Version>
{
    private final Integer majorVersion;

    private final Integer minorVersion;

    private final Integer incrementalVersion;

    private final Integer buildNumber;

    private final String qualifier;

    private final ComparableVersion comparable;

    private Version(Integer majorVersion, Integer minorVersion, Integer incrementalVersion, Integer buildNumber, String qualifier, ComparableVersion comparable) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.incrementalVersion = incrementalVersion;
        this.buildNumber = buildNumber;
        this.qualifier = qualifier;
        this.comparable = checkNotNull(comparable);
    }

    @Override
    public int hashCode()
    {
        return 11 + comparable.hashCode();
    }

    @Override
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof Version) )
        {
            return false;
        }

        return compareTo( (Version) other ) == 0;
    }

    public int compareTo( Version otherVersion )
    {
        return this.comparable.compareTo( ( (Version) otherVersion ).comparable );
    }

    public int getMajorVersion()
    {
        return majorVersion != null ? majorVersion : 0;
    }

    public int getMinorVersion()
    {
        return minorVersion != null ? minorVersion : 0;
    }

    public int getIncrementalVersion()
    {
        return incrementalVersion != null ? incrementalVersion : 0;
    }

    public int getBuildNumber()
    {
        return buildNumber != null ? buildNumber : 0;
    }

    public String getQualifier()
    {
        return qualifier;
    }

    public static Version parseVersion(String version )
    {
        ComparableVersion comparable = new ComparableVersion( version );
        Integer majorVersion = null;

        Integer minorVersion = null;

        Integer incrementalVersion = null;

        Integer buildNumber = null;

        String qualifier = null;

        int index = version.indexOf( '-' );

        String part1;
        String part2 = null;

        if ( index < 0 )
        {
            part1 = version;
        }
        else
        {
            part1 = version.substring( 0, index );
            part2 = version.substring( index + 1 );
        }

        if ( part2 != null )
        {
            try
            {
                if ( ( part2.length() == 1 ) || !part2.startsWith( "0" ) )
                {
                    buildNumber = Integer.valueOf( part2 );
                }
                else
                {
                    qualifier = part2;
                }
            }
            catch ( NumberFormatException e )
            {
                qualifier = part2;
            }
        }

        if ( ( !part1.contains( "." ) ) && !part1.startsWith( "0" ) )
        {
            try
            {
                majorVersion = Integer.valueOf( part1 );
            }
            catch ( NumberFormatException e )
            {
                // qualifier is the whole version, including "-"
                qualifier = version;
                buildNumber = null;
            }
        }
        else
        {
            boolean fallback = false;

            StringTokenizer tok = new StringTokenizer( part1, "." );
            try
            {
                majorVersion = getNextIntegerToken( tok );
                if ( tok.hasMoreTokens() )
                {
                    minorVersion = getNextIntegerToken( tok );
                }
                if ( tok.hasMoreTokens() )
                {
                    incrementalVersion = getNextIntegerToken( tok );
                }
                if ( tok.hasMoreTokens() )
                {
                    qualifier = tok.nextToken();
                    fallback = Pattern.compile( "\\d+" ).matcher( qualifier ).matches();
                }

                // string tokenizer won't detect these and ignores them
                if ( part1.contains( ".." ) || part1.startsWith( "." ) || part1.endsWith( "." ) )
                {
                    fallback = true;
                }
            }
            catch ( NumberFormatException e )
            {
                fallback = true;
            }

            if ( fallback )
            {
                // qualifier is the whole version, including "-"
                qualifier = version;
                majorVersion = null;
                minorVersion = null;
                incrementalVersion = null;
                buildNumber = null;
            }
        }
        return new Version(majorVersion, minorVersion, incrementalVersion, buildNumber, qualifier, comparable);
    }

    private static Integer getNextIntegerToken( StringTokenizer tok )
    {
        try
        {
            String s = tok.nextToken();
            if ( ( s.length() > 1 ) && s.startsWith( "0" ) )
            {
                throw new NumberFormatException( "Number part has a leading 0: '" + s + "'" );
            }
            return Integer.valueOf( s );
        }
        catch ( NoSuchElementException e )
        {
            throw new NumberFormatException( "Number is invalid" );
        }
    }

    @Override
    public String toString()
    {
        return comparable.toString();
    }

    /**
     * <p>
     * Generic implementation of version comparison.
     * </p>
     *
     * Features:
     * <ul>
     * <li>mixing of '<code>-</code>' (hyphen) and '<code>.</code>' (dot) separators,</li>
     * <li>transition between characters and digits also constitutes a separator:
     *     <code>1.0alpha1 =&gt; [1, 0, alpha, 1]</code></li>
     * <li>unlimited number of version components,</li>
     * <li>version components in the text can be digits or strings,</li>
     * <li>strings are checked for well-known qualifiers and the qualifier ordering is used for version ordering.
     *     Well-known qualifiers (case insensitive) are:<ul>
     *     <li><code>alpha</code> or <code>a</code></li>
     *     <li><code>beta</code> or <code>b</code></li>
     *     <li><code>milestone</code> or <code>m</code></li>
     *     <li><code>rc</code> or <code>cr</code></li>
     *     <li><code>snapshot</code></li>
     *     <li><code>(the empty string)</code> or <code>ga</code> or <code>final</code></li>
     *     <li><code>sp</code></li>
     *     </ul>
     *     Unknown qualifiers are considered after known qualifiers, with lexical order (always case insensitive),
     *   </li>
     * <li>a hyphen usually precedes a qualifier, and is always less important than something preceded with a dot.</li>
     * </ul>
     *
     * @see <a href="https://cwiki.apache.org/confluence/display/MAVENOLD/Versioning">"Versioning" on Maven Wiki</a>
     * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
     * @author <a href="mailto:hboutemy@apache.org">Hervé Boutemy</a>
     */
    private static class ComparableVersion
            implements Comparable<ComparableVersion>
    {
        private String value;

        private String canonical;

        private ComparableVersion.ListItem items;

        private interface Item
        {
            int INTEGER_ITEM = 0;
            int STRING_ITEM = 1;
            int LIST_ITEM = 2;

            int compareTo( ComparableVersion.Item item );

            int getType();

            boolean isNull();
        }

        /**
         * Represents a numeric item in the version item list.
         */
        private static class IntegerItem
                implements ComparableVersion.Item
        {
            private static final BigInteger BIG_INTEGER_ZERO = new BigInteger( "0" );

            private final BigInteger value;

            public static final ComparableVersion.IntegerItem ZERO = new ComparableVersion.IntegerItem();

            private IntegerItem()
            {
                this.value = BIG_INTEGER_ZERO;
            }

            public IntegerItem( String str )
            {
                this.value = new BigInteger( str );
            }

            public int getType()
            {
                return INTEGER_ITEM;
            }

            public boolean isNull()
            {
                return BIG_INTEGER_ZERO.equals( value );
            }

            public int compareTo( ComparableVersion.Item item )
            {
                if ( item == null )
                {
                    return BIG_INTEGER_ZERO.equals( value ) ? 0 : 1; // 1.0 == 1, 1.1 > 1
                }

                switch ( item.getType() )
                {
                    case INTEGER_ITEM:
                        return value.compareTo( ( (ComparableVersion.IntegerItem) item ).value );

                    case STRING_ITEM:
                        return 1; // 1.1 > 1-sp

                    case LIST_ITEM:
                        return 1; // 1.1 > 1-1

                    default:
                        throw new RuntimeException( "invalid item: " + item.getClass() );
                }
            }

            public String toString()
            {
                return value.toString();
            }
        }

        /**
         * Represents a string in the version item list, usually a qualifier.
         */
        private static class StringItem
                implements ComparableVersion.Item
        {
            private static final String[] QUALIFIERS = { "alpha", "beta", "milestone", "rc", "snapshot", "", "sp" };

            @SuppressWarnings( "checkstyle:constantname" )
            private static final List<String> _QUALIFIERS = Arrays.asList( QUALIFIERS );

            private static final Properties ALIASES = new Properties();
            static
            {
                ALIASES.put( "ga", "" );
                ALIASES.put( "final", "" );
                ALIASES.put( "cr", "rc" );
            }

            /**
             * A comparable value for the empty-string qualifier. This one is used to determine if a given qualifier makes
             * the version older than one without a qualifier, or more recent.
             */
            private static final String RELEASE_VERSION_INDEX = String.valueOf( _QUALIFIERS.indexOf( "" ) );

            private String value;

            @SuppressWarnings("BooleanParameter")
            public StringItem(String value, boolean followedByDigit )
            {
                if ( followedByDigit && value.length() == 1 )
                {
                    // a1 = alpha-1, b1 = beta-1, m1 = milestone-1
                    switch ( value.charAt( 0 ) )
                    {
                        case 'a':
                            value = "alpha";
                            break;
                        case 'b':
                            value = "beta";
                            break;
                        case 'm':
                            value = "milestone";
                            break;
                        default:
                    }
                }
                this.value = ALIASES.getProperty( value , value );
            }

            public int getType()
            {
                return STRING_ITEM;
            }

            public boolean isNull()
            {
                return ( comparableQualifier( value ).compareTo( RELEASE_VERSION_INDEX ) == 0 );
            }

            /**
             * Returns a comparable value for a qualifier.
             *
             * This method takes into account the ordering of known qualifiers then unknown qualifiers with lexical
             * ordering.
             *
             * just returning an Integer with the index here is faster, but requires a lot of if/then/else to check for -1
             * or QUALIFIERS.size and then resort to lexical ordering. Most comparisons are decided by the first character,
             * so this is still fast. If more characters are needed then it requires a lexical sort anyway.
             *
             * @param qualifier
             * @return an equivalent value that can be used with lexical comparison
             */
            public static String comparableQualifier( String qualifier )
            {
                int i = _QUALIFIERS.indexOf( qualifier );

                return i == -1 ? ( _QUALIFIERS.size() + "-" + qualifier ) : String.valueOf( i );
            }

            public int compareTo( ComparableVersion.Item item )
            {
                if ( item == null )
                {
                    // 1-rc < 1, 1-ga > 1
                    return comparableQualifier( value ).compareTo( RELEASE_VERSION_INDEX );
                }
                switch ( item.getType() )
                {
                    case INTEGER_ITEM:
                        return -1; // 1.any < 1.1 ?

                    case STRING_ITEM:
                        return comparableQualifier( value ).compareTo( comparableQualifier( ( (ComparableVersion.StringItem) item ).value ) );

                    case LIST_ITEM:
                        return -1; // 1.any < 1-1

                    default:
                        throw new RuntimeException( "invalid item: " + item.getClass() );
                }
            }

            public String toString()
            {
                return value;
            }
        }

        /**
         * Represents a version list item. This class is used both for the global item list and for sub-lists (which start
         * with '-(number)' in the version specification).
         */
        private static class ListItem
                extends ArrayList<ComparableVersion.Item>
                implements ComparableVersion.Item
        {
            public int getType()
            {
                return LIST_ITEM;
            }

            public boolean isNull()
            {
                return ( size() == 0 );
            }

            void normalize()
            {
                for ( int i = size() - 1; i >= 0; i-- )
                {
                    ComparableVersion.Item lastItem = get( i );

                    if ( lastItem.isNull() )
                    {
                        // remove null trailing items: 0, "", empty list
                        remove( i );
                    }
                    else if ( !( lastItem instanceof ComparableVersion.ListItem) )
                    {
                        break;
                    }
                }
            }

            public int compareTo( ComparableVersion.Item item )
            {
                if ( item == null )
                {
                    if ( size() == 0 )
                    {
                        return 0; // 1-0 = 1- (normalize) = 1
                    }
                    ComparableVersion.Item first = get( 0 );
                    return first.compareTo( null );
                }
                switch ( item.getType() )
                {
                    case INTEGER_ITEM:
                        return -1; // 1-1 < 1.0.x

                    case STRING_ITEM:
                        return 1; // 1-1 > 1-sp

                    case LIST_ITEM:
                        Iterator<ComparableVersion.Item> left = iterator();
                        Iterator<ComparableVersion.Item> right = ( (ComparableVersion.ListItem) item ).iterator();

                        while ( left.hasNext() || right.hasNext() )
                        {
                            ComparableVersion.Item l = left.hasNext() ? left.next() : null;
                            ComparableVersion.Item r = right.hasNext() ? right.next() : null;

                            // if this is shorter, then invert the compare and mul with -1
                            int result = l == null ? ( r == null ? 0 : -1 * r.compareTo( l ) ) : l.compareTo( r );

                            if ( result != 0 )
                            {
                                return result;
                            }
                        }

                        return 0;

                    default:
                        throw new RuntimeException( "invalid item: " + item.getClass() );
                }
            }

            public String toString()
            {
                StringBuilder buffer = new StringBuilder();
                for ( ComparableVersion.Item item : this )
                {
                    if ( buffer.length() > 0 )
                    {
                        buffer.append( ( item instanceof ComparableVersion.ListItem) ? '-' : '.' );
                    }
                    buffer.append( item );
                }
                return buffer.toString();
            }
        }

        public ComparableVersion( String version )
        {
            parseVersion( version );
        }

        public final void parseVersion( String version )
        {
            this.value = version;

            items = new ComparableVersion.ListItem();

            version = version.toLowerCase( Locale.ENGLISH );

            ComparableVersion.ListItem list = items;

            Stack<ComparableVersion.Item> stack = new Stack<>();
            stack.push( list );

            boolean isDigit = false;

            int startIndex = 0;

            for ( int i = 0; i < version.length(); i++ )
            {
                char c = version.charAt( i );

                if ( c == '.' )
                {
                    if ( i == startIndex )
                    {
                        list.add( ComparableVersion.IntegerItem.ZERO );
                    }
                    else
                    {
                        list.add( parseItem( isDigit, version.substring( startIndex, i ) ) );
                    }
                    startIndex = i + 1;
                }
                else if ( c == '-' )
                {
                    if ( i == startIndex )
                    {
                        list.add( ComparableVersion.IntegerItem.ZERO );
                    }
                    else
                    {
                        list.add( parseItem( isDigit, version.substring( startIndex, i ) ) );
                    }
                    startIndex = i + 1;

                    list.add( list = new ComparableVersion.ListItem() );
                    stack.push( list );
                }
                else if ( Character.isDigit( c ) )
                {
                    if ( !isDigit && i > startIndex )
                    {
                        list.add( new ComparableVersion.StringItem( version.substring( startIndex, i ), true ) );
                        startIndex = i;

                        list.add( list = new ComparableVersion.ListItem() );
                        stack.push( list );
                    }

                    isDigit = true;
                }
                else
                {
                    if ( isDigit && i > startIndex )
                    {
                        list.add( parseItem( true, version.substring( startIndex, i ) ) );
                        startIndex = i;

                        list.add( list = new ComparableVersion.ListItem() );
                        stack.push( list );
                    }

                    isDigit = false;
                }
            }

            if ( version.length() > startIndex )
            {
                list.add( parseItem( isDigit, version.substring( startIndex ) ) );
            }

            while ( !stack.isEmpty() )
            {
                list = (ComparableVersion.ListItem) stack.pop();
                list.normalize();
            }

            canonical = items.toString();
        }

        private static ComparableVersion.Item parseItem(boolean isDigit, String buf )
        {
            return isDigit ? new ComparableVersion.IntegerItem( buf ) : new ComparableVersion.StringItem( buf, false );
        }

        public int compareTo( ComparableVersion o )
        {
            return items.compareTo( o.items );
        }

        public String toString()
        {
            return value;
        }

        public String getCanonical()
        {
            return canonical;
        }

        public boolean equals( Object o )
        {
            return ( o instanceof ComparableVersion) && canonical.equals( ( (ComparableVersion) o ).canonical );
        }

        public int hashCode()
        {
            return canonical.hashCode();
        }

    }
}
