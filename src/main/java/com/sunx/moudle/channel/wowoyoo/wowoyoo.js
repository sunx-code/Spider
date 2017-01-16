//var params = {}

var form = document.createElement("form");
form.setAttribute("method", "post");
form.setAttribute("action", "https://wowoyoo.com/clubmed/cau");
form.setAttribute("accept-charset", "UTF-8");

for ( var key in params) {
	if (params.hasOwnProperty(key)) {
		var hiddenField = document.createElement("input");
		hiddenField.setAttribute("type", "hidden");
		hiddenField.setAttribute("name", key);
		hiddenField.setAttribute("value", params[key]);

		form.appendChild(hiddenField);
	}
}

document.body.appendChild(form);
form.submit();