#include <string>
#include <fstream>

using namespace std;
int main () {
    ofstream out("pbdata.h");
    ifstream in("data.pb", ios::in | ios::binary);
    if (out.is_open() && in.is_open())
    {

        out << "static unsigned char pbdata[] = { ";

        int flag = 0;
        int preCh;
        while (!in.eof()) {
            unsigned char ch = 0;
            in.read((char *)&ch, 1);

            if (flag == 1) {
              out << preCh;
            } else if (flag > 1) {
              out << ",";
              out << preCh;
            }
            preCh = ch;

            flag += 1;
        }

        out << "};\n";

        in.close();
        out.close();
    }
    return 0;
}
