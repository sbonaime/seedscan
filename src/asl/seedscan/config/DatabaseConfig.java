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
package asl.seedscan.config;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConfig
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.config.DatabaseConfig");

    private String uri = null;
    private String username = null;
    private Password password = null;
    private Hashtable<String, Level> levels = null;

 // constructor(s)
    public DatabaseConfig()
    {
        levels = new Hashtable<String, Level>();
    }

 // ready
    public boolean isReady()
    {
        return (uri      == null) ? false :
               (username == null) ? false :
               (password == null) ? false : true;
    }

 // uri
    public void setURI(String uri)
    {
        logger.config("URI: "+uri);
        this.uri = uri;
    }

    public String getURI()
    {
        return uri;
    }

 // username
    public void setUsername(String username)
    {
        logger.config("Username: "+username);
        this.username = username;
    }

    public String getUsername()
    {
        return username;
    }

 // password
    public void setPassword(Password password)
    {
        logger.config("Password: "+password);
        this.password = password;
    }

    public Password getPassword()
    {
        return password;
    }

 // levels
    public void setLevel(String name, String level)
    throws IllegalArgumentException
    {
        setLevel(name, Level.parse(level));
    }

    public void setLevel(String name, Level level)
    {
        logger.config("Level: '"+name+"' -> '"+level.toString()+"'");
        levels.put(name, level);
        Logger.getLogger(name).setLevel(level);
    }

    public Level getLevel(String name)
    {
        return levels.get(name);
    }

    public Enumeration<String> getLevelNames()
    {
        return levels.keys();
    }
}
