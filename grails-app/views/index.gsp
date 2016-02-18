<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main"/>
		<title>Profile services</title>
	</head>
	<body>
			<h1>Profile services</h1>
			<div id="controller-list" role="navigation">
				<p>All services require an Access Token for the target collection. Access tokens can be generated by the Collection Administrator.</p>
				<h2>Available Public Services:</h2>
				<ul>
					<li>
                        /api/v1/opus/[opusId]/count[?includeArchived=false] - count all profiles in the collection (optionally include archived profiles - defaults to false)
                    </li>
					<li>
                        /api/v1/opus/[opusId]/export[?max=1000&offset=0&includeArchived=false] - export all profiles in the collection in blocks
                        <ul>
                            <li>max - The maximum number of profiles to retrieve (profiles will be sorted alphabetically by name). Default = 500.</li>
                            <li>offset - The 0-based index of the profile to start from (profiles will be sorted alphabetically by name). Default = 0.</li>
                            <li>includeArchived - True to include archived profiles in the export. Default = false.</li>
                        </ul>
                    </li>
				</ul>
			</div>
	</body>
</html>
