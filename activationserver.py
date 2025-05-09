# Import the Flask module
from flask import Flask

# Create an instance of the Flask class
# __name__ is a special Python variable that gets the name of the current module
# Flask uses this to know where to look for resources like templates and static files.
flaskApp = Flask(__name__)

# Define the string that the server will always respond with
# You can change this to any string you like.
RESPONSE_STRING = "This is the fixed response from the KSTRADER machine activation service."

# Define a route for the specific path
# This decorator tells Flask that the function `handleActivationRequest`
# should be called when a request comes in for the URL:
# /lks410svcs/activation/kstradermachine/v1
# The <path:ignoredPathParams> part is a catch-all for any additional path segments,
# and we also use **kwargs to catch any query parameters.
# Both will be ignored by the function logic.
@flaskApp.route('/lks410svcs/activation/kstradermachine/v1', methods=['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS', 'HEAD'])
@flaskApp.route('/lks410svcs/activation/kstradermachine/v1/<path:ignoredPathParams>', methods=['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS', 'HEAD'])
def handleActivationRequest(ignoredPathParams=None, **kwargs):
    """
    Handles incoming requests to the specified path.

    This function is designed to catch all requests to the endpoint:
    /lks410svcs/activation/kstradermachine/v1
    and any subpaths. It will ignore any path parameters or query parameters
    and always return the predefined RESPONSE_STRING.

    Args:
        ignoredPathParams (str, optional): Catches any additional path segments.
                                         Defaults to None. Ignored by the function.
        **kwargs: Catches any query parameters. Ignored by the function.

    Returns:
        str: The predefined response string.
    """
    # Print a message to the console indicating a request was received (optional, for logging)
    print(f"Received request on /lks410svcs/activation/kstradermachine/v1. Path params: {ignoredPathParams}, Query params: {kwargs}")
    # Return the predefined response string
    return RESPONSE_STRING

# Check if the script is being run directly (not imported as a module)
if __name__ == '__main__':
    # Run the Flask development server
    # host='0.0.0.0' makes the server accessible from any IP address on the machine (e.g., localhost, 127.0.0.1, or its network IP)
    # port=36900 sets the port number the server will listen on
    # debug=True enables Flask's debugger, which is useful during development
    # but should be turned off in a production environment.
    print("Starting Flask server on http://localhost:36900/lks410svcs/activation/kstradermachine/v1")
    flaskApp.run(host='0.0.0.0', port=36900, debug=True)