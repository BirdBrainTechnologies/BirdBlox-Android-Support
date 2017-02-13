package com.birdbraintechnologies.birdblocks;

import java.util.List;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by tsun on 2/12/17.
 */

public interface RequestHandler {

    /**
     * Handles a request
     * @param session HttpSession generated
     * @param args List of arguments generated by regex matching
     * @return Response to the requeset
     */
    NanoHTTPD.Response handleRequest(NanoHTTPD.IHTTPSession session, List<String> args);
}
