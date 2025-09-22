console = {
  _format: function(values) {
    var msg = [];
    for (var i=0;i<values.length;i++)
      msg.push(JSON.stringify(values[i]));
    return msg.join(', ');
  },
  info: function() {
    console._format(arguments);
  },
  warn: function() {
    console._format(arguments);
  },
  debug: function() {
	console._format(arguments);
  },
  error: function() {
	console._format(arguments);
  },
  log: function() {
		console._format(arguments);
  }
};