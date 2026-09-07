<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>author-web Index page</title>
    <link rel="stylesheet" type="text/css" href="<c:url value="/webspring.css" />" />
</head>
<body>

<h3>author-web Index page</h3>

<p>
<a href="<c:url value="/sapp/landing?cwid=1&userid=1" />">CW Landing Page for CW id = 1 and user id = 1</a>
<br />
<a href="<c:url value="/sapp/product?cwid=1" />">Product Page for CW id = 1</a>
</p>

</body>
</html>
