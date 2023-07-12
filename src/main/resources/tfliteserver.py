import http.server
import json
import numpy as np # tested with numpy 1.23.5
import tensorflow as tf # tested with tensorflow-macos 2.12.0

def get_output(input_array):
    # model_path = '/Users/lazar/IdeaProjects/EGNonBinary/src/main/resources/dktscore230607conv_ff_5.tflite'
    # model_path = '/Users/lazar/IdeaProjects/EGNonBinary/src/main/resources/dktscore230607conv_ff_5_base_tail.tflite'
    model_path = '/Users/lazar/IdeaProjects/EGBinary/src/main/resources/dkt230712_base_tail.tflite'
    interpreter = tf.lite.Interpreter(model_path)

    input_details = interpreter.get_input_details()[0]
    output_details = interpreter.get_output_details()[0]

    expected_input_shape = input_details['shape']

    input_array = np.array(input_array, dtype=np.float32)
    actual_input_shape = input_array.shape
    print('input_array.shape:', input_array.shape)
    if len(actual_input_shape) != len(expected_input_shape):
        print('len(actual_input_shape):', len(actual_input_shape), ' not equal to len(expected_input_shape):',
              len(expected_input_shape))
        return np.array([[[]]])

    equality_condition = np.array_equal(input_array.shape[1:], input_details['shape'][1:])
    if not equality_condition:
        return np.array([[[]]])

    if actual_input_shape[0] != 1:
        interpreter.resize_tensor_input(0, actual_input_shape)

    interpreter.allocate_tensors()
    interpreter.set_tensor(input_details['index'], input_array)
    interpreter.invoke()
    output_data = interpreter.get_tensor(output_details['index'])
    print('output_data.shape: ', output_data.shape)
    return output_data

class SimpleHTTPRequestHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)

        # Parse the input_array from the request
        input_array = json.loads(post_data.decode('utf-8'))

        # Call the get_output function with the input_array
        output_array = get_output(input_array)

        # Convert the output_array to JSON
        output_json = json.dumps(output_array.tolist())

        # Send response headers
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()

        # Send the output_json as the response
        self.wfile.write(output_json.encode('utf-8'))

if __name__ == '__main__':
    server_address = ('', 8000)
    httpd = http.server.HTTPServer(server_address, SimpleHTTPRequestHandler)
    httpd.serve_forever()
