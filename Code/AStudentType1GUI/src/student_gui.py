from tkinter import *
from tkinter import ttk
import threading
import socket

HOST = "localhost"  # Păstrează localhost dacă serverul StudentMicroservice rulează local
STUDENT_PORT = 1700  # Modifică portul pentru StudentMicroservice (ar trebui să fie portul corect)

MESSAGE_TYPE_ALL = "toti"
MESSAGE_TYPE_2 = "tip2"
MESSAGE_TYPE_3 = "tip3"
MESSAGE_TEACHER = "teacher"

def resolve_question(question_text, message_type):
    # creare socket TCP
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    # incercare de conectare catre microserviciul StudentMicroservice
    try:
        sock.connect((HOST, STUDENT_PORT))  # Conectare la StudentMicroservice, nu Teacher

        # construirea mesajului in functie de tipul selectat
        if message_type == MESSAGE_TYPE_ALL:
            message = "toti~" + question_text
        elif message_type == MESSAGE_TYPE_2:
            message = "tip2~" + question_text
        elif message_type == MESSAGE_TYPE_3:
            message = "tip3~" + question_text
        elif message_type == MESSAGE_TEACHER:
            message = "teacher~" + question_text
        else:
            message = question_text
        print("Message type: " + message)
        # transmitere intrebare - se deleaga intrebarea catre StudentMicroservice
        sock.send(bytes(message + "\n", "utf-8"))

        # primire raspuns -> StudentMicroservice foloseste coregrafia de microservicii pentru a trimite raspunsul inapoi
        response_text = str(sock.recv(1024), "utf-8").split(" ")[2]

    except ConnectionError as e:
        # in cazul unei erori de conexiune, se afiseaza un mesaj
        response_text = f"Eroare de conectare la microserviciul StudentMicroservice! {e}"

    # se adauga raspunsul primit in caseta text din interfata grafica
    response_widget.insert(END, response_text)


def ask_question():
    # preluare text intrebare de pe interfata grafica
    question_text = question.get()

    # preluare tip mesaj din ComboBox
    message_type = message_type_combobox.get()

    # pornire thread separat pentru tratarea intrebarii respective
    # astfel, nu se blocheaza interfata grafica!
    threading.Thread(target=resolve_question, args=(question_text, message_type)).start()


if __name__ == '__main__':
    # elementul radacina al interfetei grafice
    root = Tk()
    root.title("Interactiune studenti-studenti")

    # la redimensionarea ferestrei, cadrele se extind pentru a prelua spatiul ramas
    root.columnconfigure(0, weight=1)
    root.rowconfigure(0, weight=1)

    # cadrul care incapsuleaza intregul continut
    content = ttk.Frame(root)

    # caseta text care afiseaza raspunsurile la intrebari
    response_widget = Text(content, height=10, width=50)

    # eticheta text din partea dreapta
    question_label = ttk.Label(content, text="Studentul intreaba:")

    # caseta de introducere text cu care se preia intrebarea de la utilizator
    question = ttk.Entry(content, width=50)

    # ComboBox pentru selectarea tipului de mesaj
    message_type_label = ttk.Label(content, text="Alege tipul mesajului:")
    message_type_combobox = ttk.Combobox(content, values=[MESSAGE_TYPE_ALL, MESSAGE_TYPE_2, MESSAGE_TYPE_3, MESSAGE_TEACHER])
    message_type_combobox.set(MESSAGE_TYPE_ALL)  # valoarea implicita

    # butoanele din dreapta-jos
    ask = ttk.Button(content, text="Intreaba", command=ask_question)  # la apasare, se apeleaza functia ask_question
    exitbtn = ttk.Button(content, text="Iesi", command=root.destroy)  # la apasare, se iese din aplicatie

    # plasarea elementelor in layout-ul de tip grid
    content.grid(column=0, row=0)
    response_widget.grid(column=0, row=0, columnspan=3, rowspan=4)
    question_label.grid(column=3, row=0, columnspan=2)
    question.grid(column=3, row=1, columnspan=2)
    message_type_label.grid(column=3, row=2)
    message_type_combobox.grid(column=4, row=2)
    ask.grid(column=3, row=3)
    exitbtn.grid(column=4, row=3)

    # bucla principala a interfetei grafice care asteapta evenimente de la utilizator
    root.mainloop()
