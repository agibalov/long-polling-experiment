<!doctype html>
<html>
	<head>
		<title>Long Polling</title>
		<link rel="stylesheet" href="/assets/css/bootstrap.min.css">		
	    <script type="text/javascript" src="/assets/js/jquery.js"></script>
	    <script type="text/javascript" src="/assets/js/bootstrap.min.js"></script>
	</head>
	<body>
		<div class="container">
			<h1>hello</h1>
			<input type="text" id="message" class="form-control"><button type="button" id="post" class="btn btn-primary">Post</button>
			<div id="messages">
			</div>
        	<hr>
			<p>&copy; 2013 by <a href="http://loki2302.me" target="_blank">loki2302</a>
		</div>	
		
		<script type="text/javascript">
			$(function() {		
				$.ajax({ url: "/messages", dataType: "json", success: function(data) {
					appendMessages(data);
				
					console.log("initial:", data);
					var lastMessageId = data[data.length - 1].id;
					console.log("last id is: %s", lastMessageId);
					
					(function poll() {
						$.ajax({ url: "/messages/" + lastMessageId, dataType: "json", complete: poll, timeout: 30000, success: function(data) {
							appendMessages(data);
							console.log("update", data);
							lastMessageId = data[0].id;						
						}});
					})();
				}});
				
				function appendMessages(messages) {
					for(var i = 0; i < messages.length; ++i) {
						var message = messages[i];
						$("#messages").append("<p>" + message.text + "</p>");
					}							
				};
				
				$("#post").click(function() {
					postMessage();
				});				
				
				$("#message").keypress(function(e) {
					if(e.which != 13) {
						return;
					}
					
					postMessage();
				});
				
				function postMessage() {
					var message = $("#message").val();
					if(!message) {
						return;
					}
					
					$("#message").val("");
					$.ajax({ url: "/messages", type: "POST", contentType: "application/json", data: JSON.stringify({
						"text": message
					})});
				};
			});
		</script>		
	</body>	
</html>