<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main"/>
		<title>Profile services</title>
	</head>
	<body>
			<h1>Profile services</h1>
			<p class="lead">Services to support the creation of taxon profiles.</p>
			<div id="controller-list" role="navigation">
				<h2>Available Controllers:</h2>
				<ul>
					<g:each var="c" in="${grailsApplication.controllerClasses.sort { it.fullName } }">
						<li class="controller"><g:link controller="${c.logicalPropertyName}">${c.logicalPropertyName.capitalize()}</g:link></li>
					</g:each>
				</ul>
			</div>
	</body>
</html>
