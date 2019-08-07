TARGET	:=	ptree.so

DIR_SRC	:=	source
DIR_INC	:=	include
DIR_BLD	:=	build
DIR_JDK	:=	/usr/lib/jvm/jdk-12

FILES_CPP	:=	$(wildcard $(DIR_SRC)/*.cpp)
FILES_O		:=	$(patsubst $(DIR_SRC)/%.cpp,$(DIR_BLD)/%.o,$(FILES_CPP))

FLAGS_COM	:=	-Wall -Wextra -Werror -O2
FLAGS_CXX	:=	-c -fPIC -std=c++17 -fno-exceptions \
				-I$(DIR_INC) -I$(DIR_JDK)/include -I$(DIR_JDK)/include/linux \
				$(FLAGS_COM)
FLAGS_LD	:=	-shared -lusb-1.0 \
				$(FLAGS_COM)

CXX	:=	clang++
LD	:=	clang

all: bld $(TARGET)

bld:
	mkdir -p $(DIR_BLD)

$(TARGET): $(FILES_O)
	$(LD) $(FLAGS_LD) -o $@ $^

$(DIR_BLD)/%.o: $(DIR_SRC)/%.cpp
	$(CXX) $(FLAGS_CXX) -o $@ $^

clean:
	rm -fr $(TARGET) $(FILES_O)