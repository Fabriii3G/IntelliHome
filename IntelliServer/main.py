import socket
import serial

# Configuración de la conexión con Arduino
ser = serial.Serial('COM3', 9600)  # Asegúrate de que 'COM3' es el puerto correcto para tu Arduino

# Función para enviar comandos al Arduino
def envia_a_arduino(data):
    ser.write(data)

# Configuración del servidor TCP para recibir mensajes de la aplicación Android
HOST = '0.0.0.0'  # Escucha en todas las interfaces de red
PORT = 12345      # Puerto en el que escuchar

# Crear y configurar el socket del servidor
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind((HOST, PORT))
server_socket.listen(1)
print(f"Servidor escuchando en {HOST}:{PORT}")

while True:
    # Aceptar conexiones de los clientes
    client_socket, client_address = server_socket.accept()
    print(f"Conexión establecida con {client_address}")

    # Recibir el mensaje enviado por el cliente
    message = client_socket.recv(1024).decode('utf-8')
    print(f"Mensaje recibido: {message}")

    if message == "luz prendida":
        newmessage = '1'
        envia_a_arduino(newmessage)
    elif message == "luz apagada":
        newmessage = '0'
        envia_a_arduino(newmessage)
    elif message == "moviendo":
        newmessage = '2'
        envia_a_arduino(newmessage)
    elif message == "sonando":
        newmessage = '3'
        envia_a_arduino(newmessage)

    # Enviar una respuesta de confirmación al cliente Android
    response = f"Mensaje '{message}' procesado y enviado a Arduino"
    client_socket.send(response.encode('utf-8'))

    # Cerrar la conexión con el cliente
    client_socket.close()
