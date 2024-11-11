import socket

# Configuración del servidor
HOST = '0.0.0.0'  # Escucha en todas las interfaces de red
PORT = 12345      # Puerto en el que escuchar

# Crear y configurar el socket
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind((HOST, PORT))
server_socket.listen(1)
print(f"Servidor escuchando en {HOST}:{PORT}")

while True:
    # Acepta conexiones de los clientes
    client_socket, client_address = server_socket.accept()
    print(f"Conexión establecida con {client_address}")

    # Recibe el mensaje enviado por el cliente
    message = client_socket.recv(1024).decode('utf-8')
    print(f"Mensaje recibido: {message}")

    # Procesa el mensaje y envía una respuesta al cliente
    response = f"Mensaje recibido en Python: {message}"
    client_socket.send(response.encode('utf-8'))

    # Cierra la conexión con el cliente
    client_socket.close()