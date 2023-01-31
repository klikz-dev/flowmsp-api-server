<html>
<head>
    <title>FlowMSP API - HTTP ${code}</title>
</head>
<body>
<h1>HTTP ${code}</h1>
<p>${exception}</p>
<p>
<#list traces as trace>
    <span>${trace}</span>
</#list>
</p>
</body>
</html>