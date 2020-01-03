<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Resource Server</title>
    </head>
    <body>
        <h1>Resource Server </h1>
        <h2>REST interfaces</h2>
        <ul>
            <li><tt>/rest</tt>
                <ul>
                    <li><a href="rest/config"><tt>/config</tt></a>
                    </li>
                    <li><tt>/manage</tt>
                        <ul>
                            <li><a href="rest/manage/resources"><tt>/resources</tt></a>
                                <ul>
                                    <li><a href="rest/manage/resources/{id}"><tt>/{id}</tt></a>
                                        <ul>
                                            <li>
                                                <a href="rest/manage/resources/{id}/meta"><tt>/meta</tt></a>
                                            </li>
                                            <li>
                                                <a href="rest/manage/resources/{id}/content"><tt>/content</tt></a>
                                            </li>
                                            <li>
                                                <a href="rest/manage/resources/{id}/register"><tt>/register</tt></a>
                                                <ul>
                                                    <li>
                                                        <a href="rest/manage/resources/{id}/register/policy"><tt>/policy</tt></a>
                                                    </li>
                                                </ul>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                            <li><a href="rest/manage/requests"><tt>/requests</tt></a>
                                <ul>
                                    <li><a href="rest/manage/requests/{id}"><tt>/{id}</tt></a>
                                    </li>
                                </ul>
                            </li>
                            <li><a href="rest/manage/subjects"><tt>/subjects</tt></a>
                            </li>
                        </ul>
                    </li>
                    <li>/share
                        <ul>
                            <li>/resources
                                <ul>
                                    <li><a href="rest/share/resources/{id}"<tt>/{id}</tt></a>
                                        <ul>
                                            <li><a href="rest/share/resources/{id}/meta"<tt>/meta</tt></a></li>
                                            <li><a href="rest/share/resources/{id}/content"<tt>/content</tt></a></li>
                                            <li><tt>/scopes</tt></li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                            <li>/owners
                                <ul>
                                    <li>/{id}
                                        <ul>
                                            <li><a href="rest/share/owners/{id}/discover"><tt>/discover</tt></a></li>
                                            <li>/request</li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                            <li><a href="rest/share/withme"><tt>/withme</tt></a>                               
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>
    </body>
</html>
