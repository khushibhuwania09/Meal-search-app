<%@ page import="java.util.List" %>
<%@ page import="org.bson.Document" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Dashboard | Logs</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f8f9fa;
        }
        h1 {
            text-align: center;
            color: #343a40;
            margin-bottom: 20px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
            background-color: #ffffff;
            margin-bottom: 20px;
        }
        th, td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }
        th {
            background-color: #007bff;
            color: white;
            text-transform: uppercase;
        }
        tr:hover {
            background-color: #f1f1f1;
        }
        td {
            color: #495057;
        }
        .error-message {
            color: #e74c3c;
            font-weight: bold;
            text-align: center;
        }
    </style>
</head>
<body>
<h1>Dashboard | Logs</h1>

<h2>Analytics</h2>
<ul>
    <li>Total Logs: <%= request.getAttribute("totalLogs") %></li>
    <li>Top Device: <%= request.getAttribute("topDevice") %></li>
    <li>Top Searched Term: <%= request.getAttribute("topSearchedTerm") %></li>
</ul>

<h2>All Searched Terms</h2>
<table>
    <%
        String logData = (String) request.getAttribute("logData");
        if (logData != null) {
    %>
    <%= logData %>
    <%
    } else {
    %>
    <tr><td colspan='2' class='error-message'>No logs available or an error occurred.</td></tr>
    <%
        }
    %>
</table>

<h2>Unique User Agents</h2>
<table>
    <%
        String userAgentData = (String) request.getAttribute("userAgentData");
        if (userAgentData != null) {
    %>
    <%= userAgentData %>
    <%
    } else {
    %>
    <tr><td class='error-message'>No user agents available or an error occurred.</td></tr>
    <%
        }
    %>
</table>

<%
    String errorMessage = (String) request.getAttribute("errorMessage");
    if (errorMessage != null) {
%>
<p class="error-message"><%= errorMessage %></p>
<%
    }
%>
</body>
</html>