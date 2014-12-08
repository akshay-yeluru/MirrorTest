USE [srb_StageDB]
GO

/****** Object:  Table [dbo].[staff]  ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [dbo].[staff](
	[cAtrieveNumber] [varchar](10) NULL,
	[ctitle] [varchar](10) NULL,
	[clastname] [varchar](25) NULL,
	[cfirstname] [varchar](25) NULL,
	[cphonenumber1] [varchar](15) NULL,
	[cphone1unlistedflag] [varchar](1) NULL,
	[cphonenumber2] [varchar](15) NULL,
	[cphone2unlistedflag] [varchar](1) NULL,
	[cemail] [varchar](100) NULL,
	[caddress1] [varchar](30) NULL,
	[caddress2] [varchar](30) NULL,
	[ccity] [varchar](30) NULL,
	[cpostcode] [varchar](10) NULL,
	[cregion] [varchar](10) NULL,
	[ccountry] [varchar](3) NULL,
	[clocation] [varchar](4) NULL,
	[clocationName] [varchar](50) NULL,
	[cDepartment] [varchar](4) NULL,
	[cstatus] [varchar](2) NULL
) ON [PRIMARY]

GO

SET ANSI_PADDING OFF
GO

USE [srb_StageDB]
GO

/****** Object:  Table [dbo].[srblog]  ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [dbo].[srblog](
	[cmessage] [varchar](500) NULL,
	[cuser] [varchar](50) NULL,
	[ddate] [datetime] NULL,
	[icount] [int] NULL,
	[ctype] [varchar](10) NULL
) ON [PRIMARY]

GO

SET ANSI_PADDING OFF
GO

