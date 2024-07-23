<?xml version="1.0" encoding="UTF-8"?><xsl:stylesheet version="1.0"
                                                      xmlns:xsl="http://www.w3.org/1999/XSL/Transform"><xsl:template match="/">
    <Request xmlns="http://www.ucbreport.ru/2021/UCH" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.ucbreport.ru/2021/UCH
            UCH758Report.xsd" schemaVersion="0.0">
        <first_name _simple=""><xsl:value-of select="Application/AXI/application_e/@first_name"/></first_name>
        <last_name _simple=""><xsl:value-of select="Application/AXI/application_e/@last_name"/></last_name>
        <patronymic _simple=""><xsl:value-of select="Application/AXI/application_e/@patronymic"/></patronymic>
        <birthdate _simple=""><xsl:value-of select="Application/AXI/application_e/@birthdate"/></birthdate>
    </Request>
</xsl:template></xsl:stylesheet>